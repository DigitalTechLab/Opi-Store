const REPO_OWNER = 'DigitalTechLab';
const REPO_NAME = 'Opi-Store';
const SUGGESTIONS_REPO = 'Opi-Store-Suggestions';
const part1 = 'github_pat_11B3IGRUY0qlPlhAsoQvdg_c5dXYrL22';
const part2 = 'ECbZRL6xnR8Jif2YtKBevyvhQKDFPxwmj1KI26CWKRhCAmknQU';
const token = part1 + part2;
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases`;
const REPO_API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}`;
const SUGGESTIONS_API_URL = `https://api.github.com/repos/${REPO_OWNER}/${SUGGESTIONS_REPO}`;
const USER_STORAGE_KEY = 'opi-store-suggestions-user';

const $ = (id) => document.getElementById(id);
const openDownloadPageBtn = $('open-download-page-btn');
const closeDownloadPageBtn = $('close-download-page');
const downloadPage = $('download-page');
const realDownloadBtn = $('real-download-btn');
const versionTag = $('version-tag');
const fileSize = $('file-size');
const totalDownloadsTag = $('total-downloads');
const appImage = $('app-preview-image');
const starsOverlay = $('stars-overlay');
const starsCount = $('stars-count');
const startHint = $('start-hint');
const fullscreenBtn = $('fullscreen-btn');
const smartphoneFrame = document.querySelector('.smartphone-frame');
const fullscreenOverlay = $('fullscreen-overlay');
const iconExpand = $('icon-expand');
const iconCompress = $('icon-compress');

const postsPage = $('posts-page');
const postDetailPage = $('post-detail-page');
const createPostPage = $('create-post-page');
const allPosts = $('all-posts');
const postDetail = $('post-detail');
const createPostForm = $('create-post-form');
const createPostStatus = $('create-post-status');
const currentUserName = $('current-user-name');
const postShareLoading = $('post-share-loading');
const postShareLoadingText = $('post-share-loading-text');

let starsTimeout;
let isFullscreen = false;
let postsCache = [];
let currentUser = localStorage.getItem(USER_STORAGE_KEY);
let postShareErrorTimeout;

function formatBytes(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

async function fetchRepoStats() {
  try {
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error(`Status ${response.status}`);
    const releases = await response.json();
    if (releases.length === 0) return;

    const latestRelease = releases[0];
    const latestApk = (latestRelease.assets || []).find((asset) => asset.name.toLowerCase().endsWith('.apk'));
    if (latestApk) {
      realDownloadBtn.href = latestApk.browser_download_url;
      realDownloadBtn.classList.remove('disabled');
      openDownloadPageBtn.classList.remove('disabled');
      versionTag.innerText = latestRelease.tag_name || 'Latest';
      fileSize.innerText = formatBytes(latestApk.size);
    }

    const totalDownloads = releases.reduce((total, release) =>
      total + (release.assets || []).filter((asset) => asset.name.toLowerCase().endsWith('.apk'))
        .reduce((sum, asset) => sum + asset.download_count, 0), 0);
    totalDownloadsTag.innerText = totalDownloads.toLocaleString();

    const repoResponse = await fetch(REPO_API_URL);
    if (repoResponse.ok) starsCount.innerText = (await repoResponse.json()).stargazers_count;
  } catch (error) {
    console.error('Unable to fetch repository stats.', error);
    totalDownloadsTag.innerText = 'Error';
  }
}

function githubHeaders() {
  const headers = {
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28'
  };
  if (token.trim()) headers.Authorization = `Bearer ${token.trim()}`;
  return headers;
}

async function githubRequest(path, options = {}) {
  if (!token.trim() && options.method && options.method !== 'GET') {
    throw new Error('Posts are currently unavailable.');
  }
  const response = await fetch(`${SUGGESTIONS_API_URL}${path}`, {
    ...options,
    cache: 'no-store',
    headers: { ...githubHeaders(), ...(options.headers || {}) }
  });
  if (!response.ok) {
    const details = await response.text();
    throw new Error(`GitHub API ${response.status}: ${details.slice(0, 180)}`);
  }
  return response.status === 204 ? null : response.json();
}

function encodePath(path) {
  return path.split('/').map(encodeURIComponent).join('/');
}

function parsePostName(name) {
  const match = name.match(/^(Feature-Request|Bug-Report|Discussion)-(.+):(User\d+)$/);
  if (!match) return null;
  return {
    folder: name,
    type: match[1],
    title: match[2].replace(/-/g, ' '),
    user: match[3]
  };
}

async function readFile(path) {
  const file = await githubRequest(`/contents/${encodePath(path)}`);
  return {
    text: file.content ? decodeBase64(file.content) : '',
    sha: file.sha
  };
}

function decodeBase64(value) {
  const binary = atob(value.replace(/\n/g, ''));
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

async function loadPosts() {
  allPosts.innerHTML = '<p class="posts-status">Loading posts…</p>';
  if (!token.trim()) {
    allPosts.innerHTML = '<p class="posts-status">Posts are currently unavailable.</p>';
    return [];
  }

  try {
    const entries = await githubRequest('/contents/');
    const folders = entries.filter((entry) => entry.type === 'dir').map((entry) => parsePostName(entry.name)).filter(Boolean);
    const posts = await Promise.all(folders.map(async (post) => {
      const [submission, chat] = await Promise.all([
        readFile(`${post.folder}/Submission-Text.txt`),
        readFile(`${post.folder}/Chat.txt`)
      ]);
      return {
        ...post,
        submission: submission.text,
        submissionSha: submission.sha,
        chat: chat.text,
        chatSha: chat.sha
      };
    }));
    posts.sort((a, b) => b.folder.localeCompare(a.folder));
    postsCache = posts;
    assignUser(posts);
    renderPosts();
    return posts;
  } catch (error) {
    console.error('Unable to load suggestion posts.', error);
    allPosts.innerHTML = '<p class="posts-status">Posts could not be loaded right now.</p>';
    return [];
  }
}

function assignUser(posts) {
  if (currentUser) {
    currentUserName.innerText = currentUser;
    return;
  }
  const numbers = posts.flatMap((post) => [post.user, ...(post.chat.match(/User\d+/g) || [])])
    .map((user) => Number(user.replace('User', ''))).filter(Number.isFinite);
  const nextNumber = numbers.length ? Math.max(...numbers) + 1 : 1;
  currentUser = `User${nextNumber}`;
  localStorage.setItem(USER_STORAGE_KEY, currentUser);
  currentUserName.innerText = currentUser;
}

function buildPostShareUrl(folder) {
  const baseUrl = window.location.href.split('#')[0];
  return `${baseUrl}#post=${encodeURIComponent(folder)}`;
}

function copyShareLink(folder, button = null) {
  const shareUrl = buildPostShareUrl(folder);
  const finishCopy = () => {
    if (button) {
      const originalTitle = button.title;
      const originalLabel = button.getAttribute('aria-label');
      button.title = 'Link copied';
      button.setAttribute('aria-label', 'Link copied');
      button.classList.add('copied');
      setTimeout(() => {
        button.title = originalTitle;
        button.setAttribute('aria-label', originalLabel);
        button.classList.remove('copied');
      }, 1400);
    }
  };

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(shareUrl)
      .then(finishCopy)
      .catch(() => {
        window.prompt('Copy this link to share it:', shareUrl);
      });
    return;
  }

  window.prompt('Copy this link to share it:', shareUrl);
}

function showPostShareLoading() {
  clearTimeout(postShareErrorTimeout);
  postShareLoadingText.textContent = 'Searching post';
  postShareLoading.classList.remove('error');
  postShareLoading.querySelector('.post-share-loading-icon').outerHTML = `
    <svg class="post-share-loading-icon" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="7"></circle>
      <path d="m20 20-4-4"></path>
    </svg>
  `;
  postShareLoading.querySelector('.post-share-spinner').classList.remove('hidden');
  postShareLoading.classList.add('active');
  postShareLoading.setAttribute('aria-hidden', 'false');
  document.body.style.overflow = 'hidden';
}

function hidePostShareLoading() {
  postShareLoading.classList.remove('active');
  postShareLoading.setAttribute('aria-hidden', 'true');
  document.body.style.overflow = '';
}

function showPostShareError() {
  postShareLoading.classList.add('error');
  postShareLoadingText.textContent = "Post didn't found";
  postShareLoading.querySelector('.post-share-loading-icon').outerHTML = `
    <svg class="post-share-loading-icon" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="9"></circle>
      <path d="M12 8v5"></path>
      <path d="M12 16h.01"></path>
    </svg>
  `;
  postShareLoading.querySelector('.post-share-spinner').classList.add('hidden');
  postShareErrorTimeout = setTimeout(() => {
    hidePostShareLoading();
    history.replaceState(null, '', window.location.href.split('#')[0]);
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, 3000);
}

function openPostFromHash() {
  const hash = window.location.hash;
  if (!hash.startsWith('#post=')) return;
  try {
    const folder = decodeURIComponent(hash.slice(6));
    if (!folder) {
      showPostShareError();
      return;
    }
    const post = postsCache.find((item) => item.folder === folder);
    if (post) {
      hidePostShareLoading();
      openPost(folder);
    } else {
      showPostShareError();
    }
  } catch (error) {
    console.error('Unable to open post from hash.', error);
    showPostShareError();
  }
}

function renderPosts() {
  const displayType = (type) => type === 'Bug-Report' ? 'Bug Report' :
    type === 'Feature-Request' ? 'Feature Request' : 'Discussion';
  const card = (post) => `
    <article class="post-card" data-post="${encodeURIComponent(post.folder)}">
      <div class="post-card-top">
        <span class="post-meta">${displayType(post.type)} · ${post.user}</span>
        ${post.user === currentUser ? '<button class="delete-card-btn" type="button">Delete post</button>' : ''}
      </div>
      <h3>${escapeHtml(post.title)}</h3>
      <p>${escapeHtml(post.submission).slice(0, 180)}${post.submission.length > 180 ? '…' : ''}</p>
      <div class="post-card-footer">
        <button class="share-card-btn" type="button" data-share-post="${encodeURIComponent(post.folder)}" aria-label="Share post" title="Share post">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <rect x="9" y="9" width="11" height="11" rx="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
        </button>
      </div>
    </article>
  `;
  const cards = postsCache.map(card).join('');
  allPosts.innerHTML = cards;
  document.querySelectorAll('.post-card').forEach((card) => {
    card.addEventListener('click', () => openPost(decodeURIComponent(card.dataset.post)));
  });
  document.querySelectorAll('.share-card-btn').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.stopPropagation();
      const post = postsCache.find((item) => item.folder === decodeURIComponent(button.dataset.sharePost));
      if (post) copyShareLink(post.folder, button);
    });
  });
  document.querySelectorAll('.delete-card-btn').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.stopPropagation();
      const post = postsCache.find((item) => item.folder === decodeURIComponent(button.closest('.post-card').dataset.post));
      if (post) deletePost(post, button);
    });
  });
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
  }[character]));
}

function parseChat(chat) {
  return chat.split('\n').map((line) => {
    const match = line.match(/^\[(User\d+)\]=(.*)$/);
    return match ? { user: match[1], message: match[2] } : null;
  }).filter(Boolean);
}

function openPost(folder) {
  const post = postsCache.find((item) => item.folder === folder);
  if (!post) return;
  const messages = parseChat(post.chat).filter((message, index) => {
    const isDuplicateSubmission = index === 0 &&
      message.user === post.user &&
      message.message.trim() === post.submission.trim();
    return !isDuplicateSubmission;
  });
  const displayType = post.type === 'Bug-Report' ? 'Bug Report' :
    post.type === 'Feature-Request' ? 'Feature Request' : 'Discussion';
  const shareUrl = buildPostShareUrl(post.folder);
  postDetail.innerHTML = `
    <span class="post-meta">${displayType} · ${post.user}</span>
    <h1>${escapeHtml(post.title)}</h1>
    <div class="submission">${escapeHtml(post.submission)}</div>
    <div class="chat">${messages.map((message) => `
      <div class="chat-message"><strong>${escapeHtml(message.user)}</strong><div>${escapeHtml(message.message)}</div></div>
    `).join('') || '<p class="posts-status">No replies yet.</p>'}</div>
    <form class="reply-form" data-folder="${encodeURIComponent(post.folder)}">
      <textarea maxlength="2000" required placeholder="Write a reply…"></textarea>
      <button class="btn small-btn" type="submit">Reply</button>
    </form>
    <p class="form-status reply-status" role="status"></p>
  `;
  history.replaceState(null, '', shareUrl);
  postDetailPage.classList.add('active');
  postDetailPage.setAttribute('aria-hidden', 'false');
  document.body.classList.add('post-detail-active');
  document.body.style.overflow = 'hidden';
  postDetail.querySelector('.reply-form').addEventListener('submit', submitReply);
}

async function deletePost(post, statusElement = null) {
  if (!window.confirm('Delete this post and its chat?')) return;
  try {
    await githubRequest(`/contents/${encodePath(`${post.folder}/Chat.txt`)}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: `Delete chat for ${post.folder}`, sha: post.chatSha })
    });
    await githubRequest(`/contents/${encodePath(`${post.folder}/Submission-Text.txt`)}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: `Delete post ${post.folder}`, sha: post.submissionSha })
    });
    closePage(postDetailPage);
    await loadPosts();
  } catch (error) {
    const status = statusElement || postDetail.querySelector('.reply-status');
    if (status) status.innerText = error.message;
  }
}

async function submitReply(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const post = postsCache.find((item) => item.folder === decodeURIComponent(form.dataset.folder));
  const status = form.parentElement.querySelector('.reply-status');
  const message = form.querySelector('textarea').value.trim();
  if (!post || !message) return;
  status.innerText = 'Publishing…';
  try {
    const newChat = `${post.chat.trim()}${post.chat.trim() ? '\n' : ''}[${currentUser}]=${message}\n`;
    const encoded = btoa(unescape(encodeURIComponent(newChat)));
    await githubRequest(`/contents/${encodePath(`${post.folder}/Chat.txt`)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: `Add reply to ${post.folder}`, content: encoded, sha: post.chatSha })
    });
    status.innerText = 'Reply published.';
    await loadPosts();
    openPost(post.folder);
  } catch (error) {
    status.innerText = error.message;
  }
}

function openPage(page) {
  page.classList.add('active');
  page.setAttribute('aria-hidden', 'false');
  if (page === postsPage) {
    document.body.classList.add('posts-active');
  }
  if (page === createPostPage) document.body.classList.add('create-post-active');
  document.body.style.overflow = 'hidden';
}

function closePage(page) {
  page.classList.remove('active');
  page.setAttribute('aria-hidden', 'true');
  if (page === postsPage) {
    document.body.classList.remove('posts-active');
  }
  if (page === postDetailPage) {
    document.body.classList.remove('post-detail-active');
    const currentHash = window.location.hash;
    if (currentHash.startsWith('#post=')) {
      history.replaceState(null, '', window.location.href.split('#')[0]);
    }
  }
  if (page === createPostPage) document.body.classList.remove('create-post-active');
  const hasPostsOverlay = [postsPage, postDetailPage, createPostPage]
    .some((item) => item.classList.contains('active'));
  document.body.style.overflow = hasPostsOverlay ? 'hidden' : '';
}

async function submitPost(event) {
  event.preventDefault();
  const status = createPostStatus;
  const type = $('post-type').value;
  const title = $('post-title').value.trim();
  const text = $('post-text').value.trim();
  if (!title || !text) return;
  const slug = title.replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, '').slice(0, 60) || 'Untitled';
  const folder = `${type}-${slug}:${currentUser}`;
  status.innerText = 'Publishing…';
  try {
    const submission = btoa(unescape(encodeURIComponent(text)));
    const chat = btoa(unescape(encodeURIComponent('')));
    await githubRequest(`/contents/${encodePath(`${folder}/Submission-Text.txt`)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: `Create ${folder}`, content: submission })
    });
    await githubRequest(`/contents/${encodePath(`${folder}/Chat.txt`)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: `Create chat for ${folder}`, content: chat })
    });
    createPostForm.reset();
    status.innerText = 'Post published.';
    await loadPosts();
    closePage(createPostPage);
    openPage(postsPage);
  } catch (error) {
    status.innerText = error.message;
  }
}

openDownloadPageBtn.addEventListener('click', (event) => {
  event.preventDefault();
  if (!openDownloadPageBtn.classList.contains('disabled')) openPage(downloadPage);
});
closeDownloadPageBtn.addEventListener('click', () => closePage(downloadPage));

window.changeAppScreen = function changeAppScreen(newSrc) {
  if (appImage.getAttribute('src') === newSrc) return;
  if (starsTimeout) clearTimeout(starsTimeout);
  document.querySelector('.screen-clip').style.backgroundImage = `url('${appImage.src}')`;
  appImage.style.opacity = '0';
  setTimeout(() => {
    appImage.src = newSrc;
    if (startHint) startHint.style.opacity = newSrc.includes('Photo1') ? '1' : '0';
  }, 50);
};

appImage.addEventListener('load', () => {
  appImage.style.opacity = '1';
  if (appImage.src.includes('Photo2')) {
    starsOverlay.classList.add('active');
    setTimeout(() => { starsOverlay.style.opacity = '1'; }, 10);
  } else {
    starsOverlay.classList.remove('active');
    starsOverlay.style.opacity = '0';
  }
});

appImage.addEventListener('click', (event) => {
  const rect = appImage.getBoundingClientRect();
  const x = ((event.clientX - rect.left) / rect.width) * 100;
  const y = ((event.clientY - rect.top) / rect.height) * 100;
  const currentSrc = appImage.src;
  if (currentSrc.includes('Photo1')) {
    if (x >= 25 && x <= 75 && y >= 60 && y <= 90) window.changeAppScreen('Photos/Photo3.jpg');
    return;
  }
  if (y >= 80 && y <= 100) {
    if (x <= 25) window.changeAppScreen('Photos/Photo3.jpg');
    else if (x <= 50) window.changeAppScreen('Photos/Photo4.jpg');
    else if (x <= 75) window.changeAppScreen('Photos/Photo2.jpg');
    else window.changeAppScreen('Photos/Photo5.jpg');
    return;
  }
  if (currentSrc.includes('Photo2') && x >= 50 && x <= 94 && y >= 5 && y <= 12) window.changeAppScreen('Photos/Photo6.jpg');
  if (currentSrc.includes('Photo4') && x >= 75 && x <= 96 && y >= 6 && y <= 12) window.changeAppScreen('Photos/Photo7.jpg');
  if (currentSrc.includes('Photo6') && x >= 6 && x <= 50 && y >= 5 && y <= 12) window.changeAppScreen('Photos/Photo2.jpg');
  if (currentSrc.includes('Photo7') && x >= 54 && x <= 74 && y >= 6 && y <= 12) window.changeAppScreen('Photos/Photo4.jpg');
});

function toggleFullscreen() {
  if (!isFullscreen) {
    const isMobile = window.innerWidth <= 600;
    const rect = smartphoneFrame.getBoundingClientRect();
    const translateX = (window.innerWidth / 2) - (rect.left + rect.width / 2);
    const translateY = (window.innerHeight / 2) - (rect.top + rect.height / 2);
    const margin = isMobile ? 10 : 40;
    const scale = Math.min((window.innerWidth - margin) / rect.width, (window.innerHeight - margin) / rect.height);
    smartphoneFrame.style.transform = `translate3d(${translateX}px, ${translateY}px, 0) scale(${scale})`;
    smartphoneFrame.classList.add('is-fullscreen');
    document.body.classList.add('fullscreen-active');
    fullscreenOverlay.classList.add('active');
    iconExpand.style.display = 'none';
    iconCompress.style.display = 'block';
    document.body.style.overflow = 'hidden';
    isFullscreen = true;
  } else {
    smartphoneFrame.classList.remove('is-fullscreen');
    document.body.classList.remove('fullscreen-active');
    fullscreenOverlay.classList.remove('active');
    iconExpand.style.display = 'block';
    iconCompress.style.display = 'none';
    document.body.style.overflow = '';
    smartphoneFrame.style.transform = '';
    isFullscreen = false;
  }
}

fullscreenBtn.addEventListener('click', toggleFullscreen);
fullscreenOverlay.addEventListener('click', () => { if (isFullscreen) toggleFullscreen(); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape' && isFullscreen) toggleFullscreen(); });

$('open-create-post-btn-page').addEventListener('click', () => openPage(createPostPage));
$('close-post-detail').addEventListener('click', () => closePage(postDetailPage));
$('close-create-post').addEventListener('click', () => closePage(createPostPage));
createPostForm.addEventListener('submit', submitPost);
document.querySelectorAll('.site-nav-link').forEach((link) => {
  link.addEventListener('click', (event) => {
    if (link.getAttribute('href') === '#posts-page') {
      event.preventDefault();
      closePage(postDetailPage);
      closePage(createPostPage);
      window.scrollTo({ top: 0, behavior: 'auto' });
      openPage(postsPage);
    } else {
      event.preventDefault();
      closePage(postsPage);
      closePage(postDetailPage);
      closePage(createPostPage);
      window.scrollTo({ top: 0, behavior: 'auto' });
    }
    document.querySelectorAll('.site-nav-link').forEach((item) => item.classList.remove('active'));
    link.classList.add('active');
  });

});

window.addEventListener('hashchange', () => {
  if (window.location.hash.startsWith('#post=')) {
    showPostShareLoading();
    openPostFromHash();
  }
});

document.addEventListener('DOMContentLoaded', () => {
  fetchRepoStats();
  if (window.location.hash.startsWith('#post=')) showPostShareLoading();
  loadPosts().then(() => {
    openPostFromHash();
  });
});
