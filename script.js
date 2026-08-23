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
const rotatingLogo = document.getElementById('rotating-logo');

// 3D Logo Setup via Blob for maximum reliability
const logoB64 = "Z2xURgIAAABIJgAAXAUAAEpTT057ImFzc2V0Ijp7ImdlbmVyYXRvciI6Iktocm9ub3MgZ2xURiBCbGVuZGVyIEkvTyB2NS4yLjM5IiwidmVyc2lvbiI6IjIuMCJ9LCJzY2VuZSI6MCwic2NlbmVzIjpbeyJuYW1lIjoiU2NlbmUiLCJub2RlcyI6WzBdfV0sIm5vZGVzIjpbeyJtZXNoIjowLCJuYW1lIjoiUGxhbmUuMDAxIiwicm90YXRpb24iOlswLjcwNDkyODMzODUyNzY3OTQsMC4wNTU0NjI3ODEzMzk4ODM4MDQsMC4wNTU0NjI3ODEzMzk4ODM4MDQsMC43MDQ5MjgyNzg5MjMwMzQ3XSwidHJhbnNsYXRpb24iOlstMS4zMDg4NjUwNzAzNDMwMTc2LDIuMzc0ODM2NjgzMjczMzE1NCwwLjQ3MDI5MTE2NzQ5NzYzNDldfV0sIm1hdGVyaWFscyI6W3siZG91YmxlU2lkZWQiOnRydWUsIm5hbWUiOiJNYXRlcmlhbC4wMDEiLCJwYnJNZXRhbGxpY1JvdWdobmVzcyI6eyJiYXNlQ29sb3JGYWN0b3IiOlswLjAyNTA0NjA1NDI3Mzg0Mzc2NSwwLjU1NzY4Mjg3MTgxODU0MjUsMC4wMjU5MTIxMzU4Mzk0NjIyOCwxXSwibWV0YWxsaWNGYWN0b3IiOjAsInJvdWdobmVzc0ZhY3RvciI6MC41fX1dLCJtZXNoZXMiOlt7Im5hbWUiOiJQbGFuZS4wMDEiLCJwcmltaXRpdmVzIjpbeyJhdHRyaWJ1dGVzIjp7IlBPU0lUSU9OIjowLCJOT1JNQUwiOjEsIlRFWENPT1JEXzAiOjJ9LCJpbmRpY2VzIjozLCJtYXRlcmlhbCI6MH1dfV0sImFjY2Vzc29ycyI6W3siYnVmZmVyVmlldyI6MCwiY29tcG9uZW50VHlwZSI6NTEyNiwiY291bnQiOjIzNCwibWF4IjpbMy40MDQzNzg0MTQxNTQwNTI3LDAuNDcwMjkyMzU5NTkwNTMwNCwzLjUzOTQ3NzM0ODMyNzYzNjddLCJtaW4iOlstNC4xNDU2MDc5NDgzMDMyMjMsLTAuNDcwMjkyMDMxNzY0OTg0MTMsLTIuOTgwOTY3MDQ0ODMwMzIyM10sInR5cGUiOiJWRUMzIn0seyJidWZm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AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8AAAAAAACAPwAAAAAAAIA/AAAAAAAAgD8DAAwADwAPABIAFQAVABgAGwAbAB4AIQAhACQAJwAnACoALQAtADAAMwAzADYAOQA5ADwAPwA/AEIARQA/AEUASAAzADkAPwAnAC0AMwAbACEAJwAPABUAGwAGAAMADwABAAkABgBwAHMAAQBtAHAAAQABAAYADwAnADMAPwA/AEgASwAnAD8ASwBtAAEADwBpAG0ADwAnAEsATgAnAE4AUQBmAGkADwBjAGYADwAbACcAUQAbAFEAVABjAA8AGwBgAGMAGwAbAFQAVwAbAFcAWgBdAGAAGwAbAFoAXQCEAIEAeAB4AHsAfgB+AHYA6ADoAOUA4gDoAOIA3gB4AH4A6ACHAIQAeACNAIoAhwCTAJAAjQCZAJYAkwCfAJwAmQClAKIAnwCrAKgApQCxAK4AqwC3ALQAsQC6ALcAsQCxAKsApQClAJ8AmQCZAJMAjQCNAIcAeAB4AOgA3gB4 AN4A2wCxAKUAmQC9ALoAsQDAAL0AsQCNAHgA2wCNANsA2ADAALEAmQDDAMAAmQCNANgA1QCNANUA0gDGAMMAmQDJAMYAmQCNANIAzwDMAMkAmQCNAM8AzADMAJkAjQAaABYAiwAaAIsAjwBrAGcA3ABrANwA4ABEAEEAtgBEALYAuQAdABkAjgAdAI4AkgBuAGoA3wBuAN8A4wBGAEMAuABGALgAuwAgABwAkQAgAJEAlQBvAGwA4QBvAOEA5ABJAEcAvABJALwAvgAjAB8AlAAjAJQAmAB0AHEA5gB0AOYA6QBNAEoAvwBNAL8AwgAlACIAlwAlAJcAmgAAAHIA5wAAAOcAdQBQAEwAwQBQAMEAxQAoACYAmwAoAJsAnQBTAE8AxABTAMQAyAArACkAngArAJ4AoAAQAA4AgwAQAIMAhQBWAFIAxwBWAMcAywAuACwAoQAuAKEAowANAAUAegANAHoAggBYAFUAygBYAMoAzQAxAC8ApAAxAKQApgAEAAgAfQAEAH0AeQBbAFkAzgBbAM4A0AA0ADIApwA0AKcAqQAHAAsAgAAHAIAAfABeAFwA0QBeANEA0wA4ADUAqgA4AKoArQAKAAIAdwAKAHcAfwBhAF8A1ABhANQA1gA7ADcArAA7AKwAsAATABEAhgATAIYAiABlAGIA1wBlANcA2gA+ADoArwA+AK8AswAXABQAiQAXAIkAjABoAGQA2QBoANkA3QBAAD0AsgBAALIAtQA\u003d";

if (rotatingLogo) {
  // Convert Base64 to Blob and set as source
  try {
    const byteCharacters = atob(logoB64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], {type: 'model/gltf-binary'});
    const url = URL.createObjectURL(blob);
    rotatingLogo.src = url;

    // Force rotation
    rotatingLogo.autoRotate = true;
    rotatingLogo.autoRotateDelay = 0;
  } catch (e) {
    console.error("Failed to load 3D model:", e);
  }

  // Enhanced hover effects
  rotatingLogo.addEventListener('mouseenter', () => {
    rotatingLogo.setAttribute('rotation-speed', '400%');
  });

  rotatingLogo.addEventListener('mouseleave', () => {
    rotatingLogo.setAttribute('rotation-speed', '20%');
  });
}

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
  appImage.src = newSrc;

  if (newSrc.includes('Photo2')) {
    starsOverlay.classList.add('active');
  } else {
    starsOverlay.classList.remove('active');
  }

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
    if (x >= 25 && x <= 75 && y >= 60 && y <= 90) {
      changeAppScreen('Photos/Photo3.jpg');
    }
  } else {
    if (y >= 80 && y <= 100) {
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
