/**
 * FILM-IN STREAMING APPLICATION
 * Dual-Mode Engine: Direct On-Device Local Scraping (Android) + Server API Fallback
 */

const API_BASE = 'http://localhost:5000/api';

// Application State
const state = {
  activeView: 'home',
  heroIndex: 0,
  heroTimer: null,
  heroData: [],
  myList: JSON.parse(localStorage.getItem('filmin_mylist') || '[]'),
  currentDetail: null,
  activeServer: 0,
};

// DOM Selectors
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
  initNavigation();
  initSearch();
  initModal();
  updateMyListBadge();
  loadHomeFeed();
});

/**
 * Toast Notification Utility
 */
function showToast(message, type = 'info') {
  const container = $('#toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('fade-out');
    setTimeout(() => toast.remove(), 400);
  }, 3000);
}

function updateMyListBadge() {
  const badge = $('#listBadge');
  if (badge) badge.textContent = state.myList.length;
}

function toggleBookmark(item) {
  const index = state.myList.findIndex((m) => m.slug === item.slug);
  if (index >= 0) {
    state.myList.splice(index, 1);
    showToast(`Dihapus dari List Saya`);
  } else {
    state.myList.push(item);
    showToast(`Disimpan ke List Saya`, 'success');
  }
  localStorage.setItem('filmin_mylist', JSON.stringify(state.myList));
  updateMyListBadge();
}

/**
 * Direct On-Device Local HTML Parser (No Vercel / No Server Required)
 */
const LocalScraper = {
  fetchPage: (path) => {
    if (window.AndroidScraper && typeof window.AndroidScraper.fetchHtml === 'function') {
      return window.AndroidScraper.fetchHtml(path);
    }
    return null;
  },

  cleanText: (str) => {
    if (!str) return '';
    return str.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
  },

  parseHome: (html) => {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');

    const movies = [];
    const series = [];
    const seen = new Set();

    doc.querySelectorAll('a[href^="/movie/"]').forEach((a) => {
      const href = a.getAttribute('href');
      const slug = href.split('/').filter(Boolean).pop();
      const title = LocalScraper.cleanText(a.textContent);
      if (slug && title && !title.toLowerCase().includes('browse') && !seen.has(slug) && !title.toLowerCase().includes('nonton')) {
        seen.add(slug);
        const year = slug.match(/\d{4}$/) ? slug.match(/\d{4}$/)[0] : '2024';
        movies.push({
          id: slug, slug, title, type: 'movie', link: href,
          poster: `https://image.tmdb.org/t/p/w500/${slug}.jpg`,
          year, rating: '8.2'
        });
      }
    });

    doc.querySelectorAll('a[href^="/series/"]').forEach((a) => {
      const href = a.getAttribute('href');
      const slug = href.split('/').filter(Boolean).pop();
      const title = LocalScraper.cleanText(a.textContent);
      if (slug && title && !title.toLowerCase().includes('browse') && !seen.has(slug) && !title.toLowerCase().includes('nonton')) {
        seen.add(slug);
        series.push({
          id: slug, slug, title, type: 'series', link: href,
          poster: `https://image.tmdb.org/t/p/w500/${slug}.jpg`,
          year: '2024', rating: '8.6'
        });
      }
    });

    const hero = movies.slice(0, 5).map((m, idx) => ({
      ...m,
      rating: (8.8 - idx * 0.2).toFixed(1),
      quality: '4K Ultra HD',
      synopsis: `Nonton & streaming ${m.title} Subtitle Indonesia gratis di FilmIn.`,
      backdrop: `https://image.tmdb.org/t/p/w1280/${m.slug}.jpg`,
    }));

    return { hero, trending: movies.slice(0, 15), series: series.slice(0, 15) };
  },

  parseDetail: (html, slug, type) => {
    let metadata = {};
    const scripts = html.match(/<script type="application\/ld\+json">(.*?)<\/script>/gs) || [];
    scripts.forEach((s) => {
      try {
        const jsonStr = s.replace(/<script[^>]*>/, '').replace(/<\/script>/, '');
        const json = JSON.parse(jsonStr);
        if (Array.isArray(json)) {
          json.forEach((item) => { if (item['@type'] === 'Movie') metadata = item; });
        } else if (json['@type'] === 'Movie') {
          metadata = json;
        }
      } catch (e) {}
    });

    const title = metadata.name || slug.replace(/-/g, ' ').toUpperCase();
    const synopsis = metadata.description || `Nonton film ${title} kualitas HD Subtitle Indonesia gratis di FilmIn.`;
    const images = metadata.image || [];
    const poster = Array.isArray(images) && images.length > 0 ? images[0] : `https://image.tmdb.org/t/p/w500/${slug}.jpg`;
    const backdrop = Array.isArray(images) && images.length > 1 ? images[1] : poster;
    const genres = metadata.genre || ['Action', 'Drama'];

    const servers = [
      { name: 'Server 1 (IDLIX Stream)', url: `https://z2.idlixku.com/${type}/${slug}?play=1`, quality: '1080p HD' },
      { name: 'Server 2 (VidSrc HD)', url: `https://vidsrc.me/embed/movie?imdb=${slug}`, quality: '1080p' },
      { name: 'Server 3 (AutoEmbed)', url: `https://autoembed.co/movie/${slug}`, quality: '720p HD' },
      { name: 'Server 4 (SmashyStream)', url: `https://player.smashystream.com/movie/${slug}`, quality: 'HD Multi-sub' }
    ];

    return {
      id: slug, slug, title: LocalScraper.cleanText(title), type, synopsis: LocalScraper.cleanText(synopsis),
      poster, backdrop, year: '2024', duration: '115 Menit', rating: '8.4',
      genres: Array.isArray(genres) ? genres : [genres], director: 'Lin Zhenzhao', cast: ['Actor 1', 'Actor 2'], servers
    };
  }
};

/**
 * Render Movie Card Element
 */
function createMovieCard(item) {
  const card = document.createElement('div');
  card.className = 'movie-card';
  card.dataset.slug = item.slug;
  card.dataset.type = item.type || 'movie';

  const isBookmarked = state.myList.some((m) => m.slug === item.slug);

  card.innerHTML = `
    <div class="card-poster-wrapper">
      <img src="${item.poster}" alt="${item.title}" class="card-poster" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?q=80&w=500&auto=format&fit=crop'">
      <div class="card-play-overlay">
        <div class="play-btn-circle"><i class="fa-solid fa-play"></i></div>
      </div>
      <span class="card-rating-badge"><i class="fa-solid fa-star"></i> ${item.rating || '8.0'}</span>
      <span class="card-type-badge">${item.type === 'series' ? 'SERIES' : 'HD'}</span>
    </div>
    <div class="card-info">
      <h3 class="card-title" title="${item.title}">${item.title}</h3>
      <div class="card-meta">
        <span>${item.year || '2024'}</span>
        <i class="${isBookmarked ? 'fa-solid' : 'fa-regular'} fa-bookmark text-accent bookmark-icon"></i>
      </div>
    </div>
  `;

  card.addEventListener('click', (e) => {
    if (e.target.closest('.bookmark-icon')) {
      e.stopPropagation();
      toggleBookmark(item);
      const icon = card.querySelector('.bookmark-icon');
      icon.className = state.myList.some((m) => m.slug === item.slug) ? 'fa-solid fa-bookmark text-accent bookmark-icon' : 'fa-regular fa-bookmark text-accent bookmark-icon';
      return;
    }
    openDetailModal(item.slug, item.type || 'movie');
  });

  return card;
}

/**
 * Load Home Feed Data (Direct Local Scraping / Server API Fallback)
 */
async function loadHomeFeed() {
  try {
    // 1. Try Direct On-Device Local Scraping first if on Android
    const localHtml = LocalScraper.fetchPage('/');
    if (localHtml && localHtml.length > 500) {
      console.log('⚡ Direct On-Device Local Scraping Used!');
      const data = LocalScraper.parseHome(localHtml);
      renderFeedData(data);
      return;
    }

    // 2. Fallback to API backend
    const res = await fetch(`${API_BASE}/home`);
    const json = await res.json();

    if (json.success && json.data) {
      renderFeedData(json.data);
    } else {
      renderFallbackData();
    }
  } catch (error) {
    console.error('Error loading home feed:', error);
    renderFallbackData();
  }
}

function renderFeedData({ hero, trending, series }) {
  if (hero && hero.length > 0) {
    state.heroData = hero;
    renderHeroSlide(0);
    startHeroTimer();
  }

  const trendingRow = $('#trendingRow');
  if (trendingRow) {
    trendingRow.innerHTML = '';
    (trending || []).forEach((movie) => trendingRow.appendChild(createMovieCard(movie)));
  }

  const seriesRow = $('#seriesRow');
  if (seriesRow) {
    seriesRow.innerHTML = '';
    (series || []).forEach((s) => seriesRow.appendChild(createMovieCard(s)));
  }
}

function renderHeroSlide(index) {
  const item = state.heroData[index];
  if (!item) return;

  state.heroIndex = index;
  const carousel = $('#heroCarousel');
  if (!carousel) return;

  carousel.style.backgroundImage = `url('${item.backdrop || item.poster}')`;
  $('#heroTitle').textContent = item.title;
  $('#heroSynopsis').textContent = item.synopsis || `Streaming ${item.title} Kualitas HD Subtitle Indonesia gratis di FilmIn.`;
  $('#heroRating').innerHTML = `<i class="fa-solid fa-star"></i> ${item.rating || '8.5'}`;
  $('#heroYear').textContent = item.year || '2024';
  $('#heroType').textContent = item.type === 'series' ? 'SERIAL TV' : 'FILM HD';

  $('#heroPlayBtn').onclick = () => openDetailModal(item.slug, item.type || 'movie');
  $('#heroInfoBtn').onclick = () => openDetailModal(item.slug, item.type || 'movie');
  $('#heroBookmarkBtn').onclick = () => toggleBookmark(item);
}

function startHeroTimer() {
  if (state.heroTimer) clearInterval(state.heroTimer);
  state.heroTimer = setInterval(() => {
    if (state.heroData.length > 0) {
      const nextIndex = (state.heroIndex + 1) % state.heroData.length;
      renderHeroSlide(nextIndex);
    }
  }, 6000);
}

/**
 * Open Movie / Series Detail Modal & Video Player
 */
async function openDetailModal(slug, type = 'movie') {
  const modal = $('#detailModal');
  const playerLoader = $('#playerLoader');
  const iframe = $('#videoIframe');

  modal.classList.add('active');
  document.body.style.overflow = 'hidden';

  playerLoader.style.display = 'flex';
  iframe.src = '';

  try {
    let detail = null;

    // 1. Try Direct On-Device Scraping on Android
    const localHtml = LocalScraper.fetchPage(`/${type}/${slug}`);
    if (localHtml && localHtml.length > 500) {
      detail = LocalScraper.parseDetail(localHtml, slug, type);
    } else {
      const endpoint = type === 'series' ? `/detail/series/${slug}` : `/detail/movie/${slug}`;
      const res = await fetch(`${API_BASE}${endpoint}`);
      const json = await res.json();
      if (json.success && json.data) detail = json.data;
    }

    if (!detail) throw new Error('Detail tidak ditemukan');
    state.currentDetail = detail;

    $('#modalBanner').style.backgroundImage = `url('${detail.backdrop || detail.poster}')`;
    $('#modalTitle').textContent = detail.title;
    $('#modalSynopsis').textContent = detail.synopsis || 'Sinopsis belum tersedia untuk judul ini.';
    $('#modalRating').innerHTML = `<i class="fa-solid fa-star"></i> ${detail.rating || '8.2'}`;
    $('#modalYear').textContent = detail.year || '2024';
    $('#modalDuration').textContent = detail.duration || '115 Menit';
    $('#modalDirector').textContent = detail.director || 'Lin Zhenzhao';

    const genresContainer = $('#modalGenres');
    genresContainer.innerHTML = '';
    (detail.genres || ['Action', 'Drama']).forEach((g) => {
      const badge = document.createElement('span');
      badge.className = 'badge badge-quality';
      badge.textContent = g;
      genresContainer.appendChild(badge);
    });

    renderPlayerServers(detail);
  } catch (error) {
    console.error('Error opening detail modal:', error);
    playerLoader.innerHTML = `<span class="text-red"><i class="fa-solid fa-triangle-exclamation"></i> Gagal memuat pemutar stream. Coba server alternatif.</span>`;
  }
}

function renderPlayerServers(detail) {
  const container = $('#serverTabs');
  const iframe = $('#videoIframe');
  const loader = $('#playerLoader');
  const status = $('#currentServerName');

  container.innerHTML = '';
  const servers = detail.servers || [
    { name: 'Server 1 (IDLIX Stream)', url: `https://z2.idlixku.com/movie/${detail.slug}?play=1` },
    { name: 'Server 2 (VidSrc HD)', url: `https://vidsrc.me/embed/movie?imdb=${detail.slug}` },
    { name: 'Server 3 (AutoEmbed)', url: `https://autoembed.co/movie/${detail.slug}` },
  ];

  servers.forEach((srv, idx) => {
    const tab = document.createElement('button');
    tab.className = `server-tab ${idx === 0 ? 'active' : ''}`;
    tab.textContent = srv.name;

    tab.onclick = () => {
      $$('.server-tab').forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      status.textContent = srv.name;
      loader.style.display = 'flex';
      iframe.src = srv.url;
    };

    container.appendChild(tab);
  });

  if (servers.length > 0) {
    status.textContent = servers[0].name;
    iframe.src = servers[0].url;
    iframe.onload = () => { loader.style.display = 'none'; };
  }
}

function initModal() {
  const modal = $('#detailModal');
  const closeBtn = $('#closeModalBtn');

  const closeModal = () => {
    modal.classList.remove('active');
    document.body.style.overflow = 'auto';
    $('#videoIframe').src = '';
  };

  closeBtn.onclick = closeModal;
  modal.onclick = (e) => { if (e.target === modal) closeModal(); };
}

function initSearch() {
  const input = $('#searchInput');
  const clearBtn = $('#clearSearchBtn');
  const overlay = $('#searchResultsOverlay');
  const grid = $('#searchResultsGrid');
  const title = $('#searchResultsTitle');
  let debounceTimer;

  input.addEventListener('input', (e) => {
    const query = e.target.value.trim();
    clearBtn.style.display = query ? 'block' : 'none';

    if (!query) {
      overlay.classList.remove('active');
      return;
    }

    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(async () => {
      title.textContent = `Mencari "${query}"...`;
      overlay.classList.add('active');
      grid.innerHTML = '<div class="skeleton-card"></div><div class="skeleton-card"></div>';

      try {
        const res = await fetch(`${API_BASE}/search?q=${encodeURIComponent(query)}`);
        const json = await res.json();

        grid.innerHTML = '';
        if (json.success && json.data.data.length > 0) {
          title.textContent = `Hasil Pencarian untuk "${query}" (${json.data.data.length})`;
          json.data.data.forEach((item) => grid.appendChild(createMovieCard(item)));
        } else {
          title.textContent = `Tidak ada hasil untuk "${query}"`;
        }
      } catch (e) {
        title.textContent = `Gagal melakukan pencarian`;
      }
    }, 400);
  });

  clearBtn.onclick = () => {
    input.value = '';
    clearBtn.style.display = 'none';
    overlay.classList.remove('active');
  };

  $('#closeSearchBtn').onclick = () => overlay.classList.remove('active');
}

function initNavigation() {
  $$('.nav-link, .mobile-nav-item').forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      switchView(link.dataset.target);
    });
  });

  $('#logoBtn').onclick = () => switchView('home');
}

function switchView(target) {
  const homeView = $('#homeView');
  const catalogView = $('#catalogView');

  $$('.nav-link, .mobile-nav-item').forEach((link) => {
    link.classList.toggle('active', link.dataset.target === target);
  });

  if (target === 'home') {
    homeView.classList.add('active');
    catalogView.classList.remove('active');
  } else if (target === 'movies') {
    loadCatalog('movie', 'Katalog Film');
  } else if (target === 'series') {
    loadCatalog('series', 'Katalog Serial TV');
  } else if (target === 'mylist') {
    renderMyListCatalog();
  }
}

async function loadCatalog(type, titleText) {
  const homeView = $('#homeView');
  const catalogView = $('#catalogView');
  const grid = $('#catalogGrid');
  const title = $('#catalogTitle');
  const count = $('#catalogCount');

  homeView.classList.remove('active');
  catalogView.classList.add('active');
  title.textContent = titleText;
  count.textContent = 'Memuat...';

  try {
    const res = await fetch(`${API_BASE}/${type === 'movie' ? 'movies' : 'series'}`);
    const json = await res.json();
    grid.innerHTML = '';
    if (json.success && json.data.data) {
      count.textContent = `${json.data.data.length} Judul Tersedia`;
      json.data.data.forEach((item) => grid.appendChild(createMovieCard(item)));
    }
  } catch (e) {
    count.textContent = 'Gagal memuat data';
  }
}

function renderMyListCatalog() {
  const homeView = $('#homeView');
  const catalogView = $('#catalogView');
  const grid = $('#catalogGrid');
  const title = $('#catalogTitle');
  const count = $('#catalogCount');

  homeView.classList.remove('active');
  catalogView.classList.add('active');
  title.textContent = 'List Saya (Disimpan)';
  count.textContent = `${state.myList.length} Judul Disimpan`;
  grid.innerHTML = '';

  if (state.myList.length === 0) {
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 60px; color: var(--text-muted);"><i class="fa-solid fa-bookmark" style="font-size: 48px; margin-bottom: 16px;"></i><p>Belum ada film yang disimpan ke List Saya.</p></div>`;
  } else {
    state.myList.forEach((item) => grid.appendChild(createMovieCard(item)));
  }
}

function renderFallbackData() {
  const mockMovies = [
    { slug: 'spider-man-brand-new-day-2026', title: 'Spider-Man: Brand New Day', year: '2026', rating: '8.8', poster: 'https://image.tmdb.org/t/p/w500/o8Jd8DH9oDCZfzuroJWP1f5gVNS.jpg', type: 'movie' },
    { slug: 'rurouni-kenshin-the-final-2021', title: 'Rurouni Kenshin: The Final', year: '2021', rating: '8.3', poster: 'https://image.tmdb.org/t/p/w500/l5juynjltgsQCyAoEaPKDeMYDBs.jpg', type: 'movie' },
    { slug: 'your-eyes-tell-2020', title: 'Your Eyes Tell', year: '2020', rating: '8.4', poster: 'https://image.tmdb.org/t/p/w500/cVn8E3Fxbi8HzYYtaSfsblYC4gl.jpg', type: 'movie' },
  ];

  state.heroData = mockMovies;
  renderHeroSlide(0);

  const trendingRow = $('#trendingRow');
  if (trendingRow) {
    trendingRow.innerHTML = '';
    mockMovies.forEach((m) => trendingRow.appendChild(createMovieCard(m)));
  }
}
