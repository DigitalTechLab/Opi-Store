const REPO_OWNER = 'DigitalTechLab';
const REPO_NAME = 'Opi-Store';
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases`;
const REPO_API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}`;

const openDownloadPageBtn = document.getElementById('open-download-page-btn');
const closeDownloadPageBtn = document.getElementById('close-download-page');
const downloadPage = document.getElementById('download-page');
const realDownloadBtn = document.getElementById('real-download-btn');

const versionTag = document.getElementById('version-tag');
const fileSize = document.getElementById('file-size');
const totalDownloadsTag = document.getElementById('total-downloads');
const appImage = document.getElementById('app-preview-image');
const starsOverlay = document.getElementById('stars-overlay');
const starsCount = document.getElementById('stars-count');
const startHint = document.getElementById('start-hint');

const fullscreenBtn = document.getElementById('fullscreen-btn');
const smartphoneFrame = document.querySelector('.smartphone-frame');
const fullscreenOverlay = document.getElementById('fullscreen-overlay');
const iconExpand = document.getElementById('icon-expand');
const iconCompress = document.getElementById('icon-compress');

let starsTimeout;

function formatBytes(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

async function fetchRepoStats() {
  try {
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error(`Status ${response.status}`);

    const releases = await response.json();
    if (releases.length === 0) return;

    // Latest release info
    const latestRelease = releases[0];
    const latestAssets = latestRelease.assets || [];
    const latestApk = latestAssets.find(asset => asset.name.endsWith('.apk'));

    if (latestApk) {
      realDownloadBtn.href = latestApk.browser_download_url;
      realDownloadBtn.classList.remove('disabled');
      openDownloadPageBtn.classList.remove('disabled');

      versionTag.innerText = latestRelease.tag_name || 'Latest';
      fileSize.innerText = formatBytes(latestApk.size);
    }

    // Total downloads
    let totalDownloads = 0;
    releases.forEach(release => {
      release.assets.forEach(asset => {
        if (asset.name.endsWith('.apk')) {
          totalDownloads += asset.download_count;
        }
      });
    });

    totalDownloadsTag.innerText = totalDownloads.toLocaleString();

    // Fetch Star Count
    const repoResponse = await fetch(REPO_API_URL);
    if (repoResponse.ok) {
        const repoData = await repoResponse.json();
        starsCount.innerText = repoData.stargazers_count;
    }
  } catch (error) {
    console.error("Unable to fetch repository stats.");
    totalDownloadsTag.innerText = "Error";
  }
}

openDownloadPageBtn.addEventListener('click', (e) => {
  e.preventDefault();
  if (!openDownloadPageBtn.classList.contains('disabled')) {
    downloadPage.classList.add('active');
    document.body.style.overflow = 'hidden';
  }
});

closeDownloadPageBtn.addEventListener('click', () => {
  downloadPage.classList.remove('active');
  document.body.style.overflow = '';
});

window.changeAppScreen = function(newSrc) {
  // Check if we are already on the target screen to prevent unnecessary resets
  if (appImage.getAttribute('src') === newSrc) return;

  if (starsTimeout) clearTimeout(starsTimeout);

  // Set the current image as a background to the container for a true cross-fade effect
  const screenClip = document.querySelector('.screen-clip');
  screenClip.style.backgroundImage = `url('${appImage.src}')`;

  // Start fade out animation of the foreground image
  appImage.style.opacity = '0';

  // Small delay before changing the source to allow the background to be visible
  setTimeout(() => {
    appImage.src = newSrc;

    if (!newSrc.includes('Photo1')) {
      if (startHint) startHint.style.opacity = '0';
    } else {
      if (startHint) startHint.style.opacity = '1';
    }
  }, 50);
};

// Sync overlay visibility and fade back in when the new image is ready
appImage.addEventListener('load', () => {
    // Fade the image back in over the background (cross-fade)
    appImage.style.opacity = '1';

    if (appImage.src.includes('Photo2')) {
        starsOverlay.classList.add('active');
        // Small delay for smooth opacity transition
        setTimeout(() => { starsOverlay.style.opacity = '1'; }, 10);
    } else {
        starsOverlay.classList.remove('active');
        starsOverlay.style.opacity = '0';
    }
});


appImage.addEventListener('click', (e) => {
  const rect = appImage.getBoundingClientRect();
  const x = (e.clientX - rect.left) / rect.width * 100;
  const y = (e.clientY - rect.top) / rect.height * 100;

  const currentSrc = appImage.src;

  // Screen 1: Start Button
  if (currentSrc.includes('Photo1')) {
    if (x >= 25 && x <= 75 && y >= 60 && y <= 90) {
      changeAppScreen('Photos/Photo3.jpg');
    }
    return;
  }

  // Bottom Navigation (for Screens 2-7)
  if (y >= 80 && y <= 100) {
    if (x >= 0 && x <= 25) changeAppScreen('Photos/Photo3.jpg');
    else if (x > 25 && x <= 50) changeAppScreen('Photos/Photo4.jpg');
    else if (x > 50 && x <= 75) changeAppScreen('Photos/Photo2.jpg');
    else if (x > 75 && x <= 100) changeAppScreen('Photos/Photo5.jpg');
    return;
  }

  // Screen 2 Specific: Go to Screen 6 (Codeberg Tab)
  if (currentSrc.includes('Photo2')) {
    if (x >= 50 && x <= 94 && y >= 5 && y <= 12) {
      changeAppScreen('Photos/Photo6.jpg');
    }
  }

  // Screen 4 Specific: Go to Screen 7 (Coords: 813,152 to 1043,258)
  if (currentSrc.includes('Photo4')) {
    if (x >= 75 && x <= 96 && y >= 6 && y <= 12) {
      changeAppScreen('Photos/Photo7.jpg');
    }
  }

  // Screen 6 Specific: Go back to Screen 2 (My Repositorys Tab)
  if (currentSrc.includes('Photo6')) {
    if (x >= 6 && x <= 50 && y >= 5 && y <= 12) {
      changeAppScreen('Photos/Photo2.jpg');
    }
  }

  // Screen 7 Specific: Go back to Screen 4 (Coords: 583,145 to 802,260)
  if (currentSrc.includes('Photo7')) {
    if (x >= 54 && x <= 74 && y >= 6 && y <= 12) {
      changeAppScreen('Photos/Photo4.jpg');
    }
  }
});

let isFullscreen = false;

function toggleFullscreen() {
  if (!isFullscreen) {
    const isMobile = window.innerWidth <= 600;
    const rect = smartphoneFrame.getBoundingClientRect();
    const winWidth = window.innerWidth;
    const winHeight = window.innerHeight;
    const frameCenterX = rect.left + (rect.width / 2);
    const frameCenterY = rect.top + (rect.height / 2);
    const translateX = (winWidth / 2) - frameCenterX;
    const translateY = (winHeight / 2) - frameCenterY;
    const margin = isMobile ? 10 : 40;
    const scaleX = (winWidth - margin) / rect.width;
    const scaleY = (winHeight - margin) / rect.height;
    const scale = Math.min(scaleX, scaleY);
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
fullscreenOverlay.addEventListener('click', () => {
  if (isFullscreen) toggleFullscreen();
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && isFullscreen) toggleFullscreen();
});

document.addEventListener('DOMContentLoaded', fetchRepoStats);
