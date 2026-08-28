# FilmIn 🎬

**FilmIn** adalah aplikasi web streaming film dan serial TV modern yang dirancang khusus untuk memberikan pengalaman menonton sinematik premium (Netflix / Disney+ Style) dengan sumber data scraped dari `https://z2.idlixku.com/`.

---

## ✨ Fitur Utama

- 🍿 **Tampilan Cinema Premium (Dark Theme):** Desain modern berkelas dengan efek glassmorphism, neon gold & red accent, serta tata letak responsif untuk HP, tablet, dan PC.
- 🦸 **Featured Hero Banner Carousel:** Slideshow banner film unggulan otomatis dengan poster HD, skor rating, badge kualitas 4K, dan sinopsis.
- 📡 **Multi-Server Streaming:** Pemutar video tertanam dengan pilihan server otomatis:
  - **Server 1:** IDLIX Stream
  - **Server 2:** VidSrc 1080p
  - **Server 3:** AutoEmbed HD
  - **Server 4:** SmashyStream
  - **Trailer:** YouTube Official Trailer
- 📺 **Navigasi Episode Serial TV:** Pilihan Season dan grid Episode yang mudah digunakan untuk nonton serial bersambung.
- 🔍 **Live Search & Filter Genre:** Pencarian cepat dengan preview poster instan serta filter genre (Action, Horror, Animation, Drama, Romance, Sci-Fi, Thriller, dll).
- 🔖 **List Saya (Watchlist):** Fitur penanda bookmark lokal (*LocalStorage*) untuk menyimpan film favorit.
- ⚡ **Ringan & Cepat:** Backend scraper Express + Cheerio yang responsif dan hemat sumber daya.

---

## 🛠️ Struktur Proyek

```
film-in/
├── backend/                  # Server Node.js & Scraper API
│   ├── scrapers/             # Scraper IDLIX (Cheerio + Axios)
│   │   └── idlix.scraper.js
│   ├── server.js             # Express API Server & Static File Host
│   └── package.json
├── frontend/                 # Web Application Streaming UI
│   ├── index.html            # HTML5 Single Page App
│   ├── style.css             # Glassmorphism Cinema Design System
│   └── app.js                # Frontend Controller & API Client
├── package.json              # Root package config
└── README.md
```

---

## 🚀 Cara Menjalankan Aplikasi

### 1. Jalankan Backend & Web App
Di direktori `film-in`:
```bash
npm start
```
Atau jika ingin mode development:
```bash
npm run dev
```

### 2. Buka Aplikasi di Browser
Akses aplikasi melalui browser:
👉 **[http://localhost:5000](http://localhost:5000)**

API Status Check:
👉 **[http://localhost:5000/api/health](http://localhost:5000/api/health)**

---

## 📡 Dokumentasi Endpoint API Scraper

| Method | Endpoint | Deskripsi |
| :--- | :--- | :--- |
| `GET` | `/api/home` | Feed beranda (Hero carousel, film trending, serial populer) |
| `GET` | `/api/movies` | Katalog film |
| `GET` | `/api/series` | Katalog serial TV |
| `GET` | `/api/genre/:genre` | Filter film & serial berdasarkan genre |
| `GET` | `/api/search?q=query` | Pencarian film & serial TV |
| `GET` | `/api/detail/movie/:slug` | Detail lengkap film & daftar server streaming |
| `GET` | `/api/detail/series/:slug` | Detail serial TV & daftar season/episode |
