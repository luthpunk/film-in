import { execFile } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PY_SCRIPT = path.join(__dirname, 'idlix_scraper.py');

/**
 * Execute Python Scraper Bridge
 */
const runPythonScraper = (command, param = '') => {
  return new Promise((resolve, reject) => {
    execFile('python3', [PY_SCRIPT, command, param], { maxBuffer: 10 * 1024 * 1024 }, (error, stdout, stderr) => {
      if (error) {
        console.error(`Python Scraper Error (${command}):`, stderr || error.message);
        return reject(new Error(`Gagal mengeksekusi scraper (${command})`));
      }
      try {
        const json = JSON.parse(stdout);
        if (json.error) {
          return reject(new Error(json.error));
        }
        resolve(json);
      } catch (e) {
        reject(new Error('Gagal memproses JSON dari scraper Python'));
      }
    });
  });
};

export const getHome = () => runPythonScraper('home');
export const getCatalog = (type = 'movie') => runPythonScraper('catalog', type);
export const getByGenre = (genreSlug) => runPythonScraper('genre', genreSlug);
export const searchContent = (query) => runPythonScraper('search', query);
export const getMovieDetail = (slug) => runPythonScraper('movie-detail', slug);
export const getSeriesDetail = (slug) => runPythonScraper('series-detail', slug);
