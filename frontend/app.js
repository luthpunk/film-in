/**
 * FILM-IN STREAMING WEB APPLICATION
 * Core Client Controller & API Integration
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
  selectedSeason: 1,
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
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('fade-out');
    setTimeout(() => toast.remove(), 400);
  }, 3000);
}

/**
 * Update My List Badge
 */
function updateMyListBadge() {
  const badge = $('#listBadge');
  if (badge) badge.textContent = state.myList.length;
}

/**
 * Save / Remove Item in My List
 */
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

  // Event Handlers
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
 * Load Home Feed Data
 */
async function loadHomeFeed() {
  try {
    const res = await fetch(`${API_BASE}/home`);
    const json = await res.json();

    if (!json.success || !json.data) throw new Error('Format data tidak sesuai');

    const { hero, trending, series } = json.data;

    // Render Hero Carousel
    if (hero && hero.length > 0) {
      state.heroData = hero;
      renderHeroSlide(0);
      startHeroTimer();
    }

    // Render Trending Movies Row
    const trendingRow = $('#trendingRow');
    trendingRow.innerHTML = '';
    trending.forEach((movie) => trendingRow.appendChild(createMovieCard(movie)));

    // Render Series Row
    const seriesRow = $('#seriesRow');
    seriesRow.innerHTML = '';
    series.forEach((s) => seriesRow.appendChild(createMovieCard(s)));

    // Load Genre Action & Horror Rows
    loadGenreRows();
  } catch (error) {
    console.error('Error loading home feed:', error);
    // Fallback Mock Data if Backend is offline
    renderFallbackData();
  }
}

/**
 * Load Genre Rows
 */
async function loadGenreRows() {
  try {
    const [actionRes, horrorRes] = await Promise.all([
      fetch(`${API_BASE}/genre/action`),
      fetch(`${API_BASE}/genre/horror`),
    ]);

    const actionJson = await actionRes.json();
    const horrorJson = await horrorRes.json();

    if (actionJson.success && actionJson.data.data) {
      const actionRow = $('#actionRow');
      actionRow.innerHTML = '';
      actionJson.data.data.slice(0, 10).forEach((m) => actionRow.appendChild(createMovieCard(m)));
    }

    if (horrorJson.success && horrorJson.data.data) {
      const horrorRow = $('#horrorRow');
      horrorRow.innerHTML = '';
      horrorJson.data.data.slice(0, 10).forEach((m) => horrorRow.appendChild(createMovieCard(m)));
    }
  } catch (e) {
    console.log('Genre rows fallback used');
  }
}

/**
 * Render Hero Slide
 */
function renderHeroSlide(index) {
  const item = state.heroData[index];
  if (!item) return;

  state.heroIndex = index;
  const carousel = $('#heroCarousel');

  carousel.style.backgroundImage = `url('${item.backdrop || item.poster}')`;
  $('#heroTitle').textContent = item.title;
  $('#heroSynopsis').textContent = item.synopsis || `Streaming ${item.title} Kualitas HD Subtitle Indonesia gratis di FilmIn.`;
  $('#heroRating').innerHTML = `<i class="fa-solid fa-star"></i> ${item.rating || '8.5'}`;
  $('#heroYear').textContent = item.year || '2024';
  $('#heroType').textContent = item.type === 'series' ? 'SERIAL TV' : 'FILM HD';

  // Action Buttons
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
    const endpoint = type === 'series' ? `/detail/series/${slug}` : `/detail/movie/${slug}`;
    const res = await fetch(`${API_BASE}${endpoint}`);
    const json = await res.json();

    if (!json.success || !json.data) throw new Error('Detail tidak ditemukan');

    const detail = json.data;
    state.currentDetail = detail;

    // Populate Modal Banner & Info
    $('#modalBanner').style.backgroundImage = `url('${detail.backdrop || detail.poster}')`;
    $('#modalTitle').textContent = detail.title;
    $('#modalSynopsis').textContent = detail.synopsis || 'Sinopsis belum tersedia untuk judul ini.';
    $('#modalRating').innerHTML = `<i class="fa-solid fa-star"></i> ${detail.rating || '8.2'}`;
    $('#modalYear').textContent = detail.year || '2024';
    $('#modalDuration').textContent = detail.duration || '115 Menit';
    $('#modalDirector').textContent = detail.director || 'Lin Zhenzhao';

    // Genres Tags
    const genresContainer = $('#modalGenres');
    genresContainer.innerHTML = '';
    (detail.genres || ['Action', 'Drama']).forEach((g) => {
      const badge = document.createElement('span');
      badge.className = 'badge badge-quality';
      badge.textContent = g;
      genresContainer.appendChild(badge);
    });

    // Cast Tags
    const castContainer = $('#modalCast');
    castContainer.innerHTML = '';
    (detail.cast || ['Actor 1', 'Actor 2']).forEach((actor) => {
      const tag = document.createElement('span');
      tag.className = 'cast-tag';
      tag.textContent = actor;
      castContainer.appendChild(tag);
    });

    // Render Stream Player & Server Tabs
    renderPlayerServers(detail);

    // Render Series Episodes if applicable
    const episodesSection = $('#episodesSection');
    if (type === 'series' && detail.seasons && detail.seasons.length > 0) {
      episodesSection.style.display = 'block';
      renderSeriesSeasons(detail.seasons);
    } else {
      episodesSection.style.display = 'none';
    }
  } catch (error) {
    console.error('Error opening detail modal:', error);
    playerLoader.innerHTML = `<span class="text-red"><i class="fa-solid fa-triangle-exclamation"></i> Gagal memuat pemutar stream. Coba server alternatif.</span>`;
  }
}

/**
 * Render Player Servers Tabs
 */
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

  // Set default initial server
  if (servers.length > 0) {
    status.textContent = servers[0].name;
    iframe.src = servers[0].url;
    iframe.onload = () => {
      loader.style.display = 'none';
    };
  }
}

/**
 * Render Series Season Dropdown & Episodes Grid
 */
function renderSeriesSeasons(seasons) {
  const seasonSelect = $('#seasonSelect');
  const episodesGrid = $('#episodesGrid');

  seasonSelect.innerHTML = '';
  seasons.forEach((season) => {
    const opt = document.createElement('option');
    opt.value = season.seasonNumber;
    opt.textContent = `Season ${season.seasonNumber}`;
    seasonSelect.appendChild(opt);
  });

  const renderEpisodes = (seasonNum) => {
    episodesGrid.innerHTML = '';
    const seasonData = seasons.find((s) => s.seasonNumber === Number(seasonNum)) || seasons[0];
    seasonData.episodes.forEach((ep) => {
      const epCard = document.createElement('div');
      epCard.className = 'episode-card';
      epCard.innerHTML = `<i class="fa-solid fa-play"></i> Eps ${ep.episode}`;

      epCard.onclick = () => {
        $$('.episode-card').forEach((c) => c.classList.remove('active'));
        epCard.classList.add('active');
        $('#playerLoader').style.display = 'flex';
        $('#videoIframe').src = ep.streamUrl;
      };

      episodesGrid.appendChild(epCard);
    });
  };

  seasonSelect.onchange = (e) => renderEpisodes(e.target.value);
  renderEpisodes(seasons[0].seasonNumber);
}

/**
 * Modal Controls
 */
function initModal() {
  const modal = $('#detailModal');
  const closeBtn = $('#closeModalBtn');

  const closeModal = () => {
    modal.classList.remove('active');
    document.body.style.overflow = 'auto';
    $('#videoIframe').src = '';
  };

  closeBtn.onclick = closeModal;
  modal.onclick = (e) => {
    if (e.target === modal) closeModal();
  };
}

/**
 * Search Functionality
 */
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

/**
 * Navigation View Switching
 */
function initNavigation() {
  const navLinks = $$('.nav-link, .mobile-nav-item');

  navLinks.forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const target = link.dataset.target;
      switchView(target);
    });
  });

  $('#logoBtn').onclick = () => switchView('home');

  // Genre Pills Filter
  $$('.genre-pill').forEach((pill) => {
    pill.addEventListener('click', () => {
      $$('.genre-pill').forEach((p) => p.classList.remove('active'));
      pill.classList.add('active');
      const genre = pill.dataset.genre;
      if (genre === 'all') switchView('home');
      else loadGenreCatalog(genre);
    });
  });
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
  count.textContent = 'Memuat data...';
  grid.innerHTML = '<div class="skeleton-card"></div><div class="skeleton-card"></div><div class="skeleton-card"></div><div class="skeleton-card"></div>';

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

async function loadGenreCatalog(genreSlug) {
  const homeView = $('#homeView');
  const catalogView = $('#catalogView');
  const grid = $('#catalogGrid');
  const title = $('#catalogTitle');
  const count = $('#catalogCount');

  homeView.classList.remove('active');
  catalogView.classList.add('active');

  title.textContent = `Genre: ${genreSlug.toUpperCase()}`;
  count.textContent = 'Memuat...';
  grid.innerHTML = '<div class="skeleton-card"></div><div class="skeleton-card"></div>';

  try {
    const res = await fetch(`${API_BASE}/genre/${genreSlug}`);
    const json = await res.json();

    grid.innerHTML = '';
    if (json.success && json.data.data) {
      count.textContent = `${json.data.data.length} Judul`;
      json.data.data.forEach((item) => grid.appendChild(createMovieCard(item)));
    }
  } catch (e) {
    count.textContent = 'Gagal memuat genre';
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
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 60px; color: var(--text-muted);"><i class="fa-solid fa-bookmark" style="font-size: 48px; margin-bottom: 16px;"></i><p>Belum ada film atau serial TV yang disimpan ke List Saya.</p></div>`;
  } else {
    state.myList.forEach((item) => grid.appendChild(createMovieCard(item)));
  }
}

/**
 * Fallback Data if Backend offline
 */
function renderFallbackData() {
  const mockMovies = [
    { slug: 'spider-man-brand-new-day-2026', title: 'Spider-Man: Brand New Day', year: '2026', rating: '8.8', poster: 'https://image.tmdb.org/t/p/w500/o8Jd8DH9oDCZfzuroJWP1f5gVNS.jpg', type: 'movie' },
    { slug: 'rurouni-kenshin-the-final-2021', title: 'Rurouni Kenshin: The Final', year: '2021', rating: '8.3', poster: 'https://image.tmdb.org/t/p/w500/l5juynjltgsQCyAoEaPKDeMYDBs.jpg', type: 'movie' },
    { slug: 'your-eyes-tell-2020', title: 'Your Eyes Tell', year: '2020', rating: '8.4', poster: 'https://image.tmdb.org/t/p/w500/cVn8E3Fxbi8HzYYtaSfsblYC4gl.jpg', type: 'movie' },
    { slug: 'i-am-a-hero-2016', title: 'I Am a Hero', year: '2016', rating: '7.6', poster: 'https://image.tmdb.org/t/p/w500/v5CEt88iDsuoMaW1Q5Msu9UZdEt.jpg', type: 'movie' },
  ];

  state.heroData = mockMovies;
  renderHeroSlide(0);

  const trendingRow = $('#trendingRow');
  trendingRow.innerHTML = '';
  mockMovies.forEach((m) => trendingRow.appendChild(createMovieCard(m)));
}
