const REPO_OWNER = 'DigitalTechLab';
const REPO_NAME = 'Opi-Store';
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;

const openDownloadPageBtn = document.getElementById('open-download-page-btn');
const closeDownloadPageBtn = document.getElementById('close-download-page');
const downloadPage = document.getElementById('download-page');
const realDownloadBtn = document.getElementById('real-download-btn');

const statusText = document.getElementById('status-text');
const statusDot = document.getElementById('status-dot');
const versionTag = document.getElementById('version-tag');
const fileSize = document.getElementById('file-size');
const appImage = document.getElementById('app-preview-image');
const startHint = document.getElementById('start-hint');

const fullscreenBtn = document.getElementById('fullscreen-btn');
const smartphoneFrame = document.querySelector('.smartphone-frame');
const fullscreenOverlay = document.getElementById('fullscreen-overlay');
const iconExpand = document.getElementById('icon-expand');
const iconCompress = document.getElementById('icon-compress');

function formatBytes(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

async function fetchLatestRelease() {
  try {
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error(`Status ${response.status}`);

    const data = await response.json();
    const assets = data.assets || [];
    const apkAsset = assets.find(asset => asset.name.endsWith('.apk'));

    if (apkAsset) {
      realDownloadBtn.href = apkAsset.browser_download_url;
      realDownloadBtn.classList.remove('disabled');
      openDownloadPageBtn.classList.remove('disabled');

      statusText.innerText = 'Ready to install';
      statusDot.classList.remove('pulse');
      statusDot.classList.add('active');
      versionTag.innerText = data.tag_name || 'Latest';
      fileSize.innerText = formatBytes(apkAsset.size);
    } else {
      statusText.innerText = 'No APK found in the latest release.';
      statusDot.classList.remove('pulse');
    }
  } catch (error) {
    statusText.innerText = 'Unable to fetch version data.';
    statusDot.classList.remove('pulse');
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
  appImage.src = newSrc;
  if (!newSrc.includes('Photo1')) {
    if (startHint) startHint.style.opacity = '0';
  } else {
    if (startHint) startHint.style.opacity = '1';
  }
};

appImage.addEventListener('click', (e) => {
  const rect = appImage.getBoundingClientRect();
  const x = (e.clientX - rect.left) / rect.width * 100;
  const y = (e.clientY - rect.top) / rect.height * 100;

  if (appImage.src.includes('Photo1.jpg') || appImage.src.includes('Photo1.png')) {
    if (x >= 30 && x <= 70 && y >= 65 && y <= 85) {
      changeAppScreen('Photos/Photo3.jpg');
    }
  } else {
    if (y >= 85 && y <= 99) {
      if (x >= 0 && x <= 25) changeAppScreen('Photos/Photo3.jpg');
      else if (x > 25 && x <= 50) changeAppScreen('Photos/Photo4.jpg');
      else if (x > 50 && x <= 75) changeAppScreen('Photos/Photo2.jpg');
      else if (x > 75 && x <= 100) changeAppScreen('Photos/Photo5.jpg');
    }
  }
});

let isFullscreen = false;

function toggleFullscreen() {
  if (!isFullscreen) {
    const rect = smartphoneFrame.getBoundingClientRect();
    const winWidth = window.innerWidth;
    const winHeight = window.innerHeight;
    const frameCenterX = rect.left + (rect.width / 2);
    const frameCenterY = rect.top + (rect.height / 2);

    const translateX = (winWidth / 2) - frameCenterX;
    const translateY = (winHeight / 2) - frameCenterY;

    const scaleX = (winWidth - 40) / rect.width;
    const scaleY = (winHeight - 40) / rect.height;
    const scale = Math.min(scaleX, scaleY);

    smartphoneFrame.classList.add('is-fullscreen');
    document.body.classList.add('fullscreen-active');
    fullscreenOverlay.classList.add('active');
    iconExpand.style.display = 'none';
    iconCompress.style.display = 'block';
    document.body.style.overflow = 'hidden';

    smartphoneFrame.style.transform = `translate3d(${translateX}px, ${translateY}px, 0) scale(${scale})`;
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

document.addEventListener('DOMContentLoaded', fetchLatestRelease);