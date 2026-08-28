import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import {
  getHome,
  getCatalog,
  getByGenre,
  searchContent,
  getMovieDetail,
  getSeriesDetail,
} from './scrapers/idlix.scraper.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Serve Frontend Static Files
const frontendPath = path.join(__dirname, '../frontend');
app.use(express.static(frontendPath));

// Health Check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'FilmIn Scraper API', timestamp: new Date().toISOString() });
});

// Home Page Feed
app.get('/api/home', async (req, res) => {
  try {
    const data = await getHome();
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Movies Catalog
app.get('/api/movies', async (req, res) => {
  try {
    const page = req.query.page || 1;
    const data = await getCatalog('movie', page);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Series Catalog
app.get('/api/series', async (req, res) => {
  try {
    const page = req.query.page || 1;
    const data = await getCatalog('series', page);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Genre Filter
app.get('/api/genre/:genre', async (req, res) => {
  try {
    const { genre } = req.params;
    const page = req.query.page || 1;
    const data = await getByGenre(genre, page);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Search
app.get('/api/search', async (req, res) => {
  try {
    const query = req.query.q || '';
    const data = await searchContent(query);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Movie Details
app.get('/api/detail/movie/:slug', async (req, res) => {
  try {
    const { slug } = req.params;
    const data = await getMovieDetail(slug);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// Series Details
app.get('/api/detail/series/:slug', async (req, res) => {
  try {
    const { slug } = req.params;
    const data = await getSeriesDetail(slug);
    res.json({ success: true, data });
  } catch (error) {
    res.status(500).json({ success: false, message: error.message });
  }
});

// SPA Fallback Route to index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(frontendPath, 'index.html'));
});

// Start Server
app.listen(PORT, () => {
  console.log(`\n==================================================`);
  console.log(`🎬 FILM-IN STREAMING APPLICATION RUNNING!`);
  console.log(`🌐 Website Web App: http://localhost:${PORT}`);
  console.log(`📡 Backend API:     http://localhost:${PORT}/api/health`);
  console.log(`==================================================\n`);
});
