import sys
import json
import urllib.request
import re

BASE_URL = 'https://z2.idlixku.com'
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7',
}

def fetch_html(path):
    url = BASE_URL + path if path.startswith('/') else path
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=10) as response:
        return response.read().decode('utf-8', errors='ignore')

def clean_text(text):
    if not text:
        return ''
    text = re.sub(r'<[^>]+>', '', text)
    return re.sub(r'\s+', ' ', text).strip()

def get_home():
    html = fetch_html('/')
    
    movie_matches = re.findall(r'<a[^>]+href="(/movie/[^"]+)"[^>]*>(.*?)</a>', html)
    series_matches = re.findall(r'<a[^>]+href="(/series/[^"]+)"[^>]*>(.*?)</a>', html)
    
    trending = []
    seen = set()
    
    for href, raw_title in movie_matches:
        slug = href.split('/')[-1]
        title = clean_text(raw_title)
        if slug and title and title != 'Browse Movies' and slug not in seen and not 'nonton' in title.lower():
            seen.add(slug)
            year = re.search(r'\d{4}$', slug)
            trending.append({
                'id': slug,
                'slug': slug,
                'title': title,
                'type': 'movie',
                'link': href,
                'poster': f'https://image.tmdb.org/t/p/w500/{slug}.jpg',
                'year': year.group(0) if year else '2024',
                'rating': '8.2'
            })
            
    series = []
    for href, raw_title in series_matches:
        slug = href.split('/')[-1]
        title = clean_text(raw_title)
        if slug and title and title != 'Browse TV Series' and slug not in seen and not 'nonton' in title.lower():
            seen.add(slug)
            series.append({
                'id': slug,
                'slug': slug,
                'title': title,
                'type': 'series',
                'link': href,
                'poster': f'https://image.tmdb.org/t/p/w500/{slug}.jpg',
                'year': '2024',
                'rating': '8.6'
            })
            
    hero = []
    for idx, m in enumerate(trending[:5]):
        hero.append({
            **m,
            'rating': str(round(8.8 - idx * 0.2, 1)),
            'quality': '4K Ultra HD',
            'synopsis': f'Nonton & streaming {m["title"]} Subtitle Indonesia dengan kualitas jernih HD di FilmIn.',
            'backdrop': f'https://image.tmdb.org/t/p/w1280/{m["slug"]}.jpg'
        })
        
    return {
        'hero': hero,
        'trending': trending[:15],
        'movies': trending[5:20],
        'series': series[:15]
    }

def get_catalog(cat_type):
    path = '/movie' if cat_type == 'movie' else '/series'
    html = fetch_html(path)
    pattern = r'<a[^>]+href="(/movie/[^"]+)"[^>]*>(.*?)</a>' if cat_type == 'movie' else r'<a[^>]+href="(/series/[^"]+)"[^>]*>(.*?)</a>'
    matches = re.findall(pattern, html)
    
    items = []
    seen = set()
    for href, raw_title in matches:
        slug = href.split('/')[-1]
        title = clean_text(raw_title)
        if slug and title and 'browse' not in title.lower() and slug not in seen:
            seen.add(slug)
            year = re.search(r'\d{4}$', slug)
            items.append({
                'id': slug,
                'slug': slug,
                'title': title,
                'type': cat_type,
                'link': href,
                'poster': f'https://image.tmdb.org/t/p/w500/{slug}.jpg',
                'year': year.group(0) if year else '2024',
                'rating': '7.9'
            })
    return {'type': cat_type, 'total': len(items), 'data': items}

def get_genre(genre_slug):
    html = fetch_html(f'/genre/{genre_slug}')
    matches = re.findall(r'<a[^>]+href="((?:/movie/|/series/)[^"]+)"[^>]*>(.*?)</a>', html)
    
    items = []
    seen = set()
    for href, raw_title in matches:
        slug = href.split('/')[-1]
        title = clean_text(raw_title)
        cat_type = 'movie' if href.startswith('/movie/') else 'series'
        if slug and title and 'browse' not in title.lower() and slug not in seen:
            seen.add(slug)
            items.append({
                'id': slug,
                'slug': slug,
                'title': title,
                'type': cat_type,
                'link': href,
                'poster': f'https://image.tmdb.org/t/p/w500/{slug}.jpg',
                'rating': '7.8'
            })
    return {'genre': genre_slug, 'total': len(items), 'data': items}

def search(query):
    if not query:
        return {'query': '', 'total': 0, 'data': []}
    q_lower = query.lower().strip()
    
    movies = get_catalog('movie')['data']
    series = get_catalog('series')['data']
    combined = movies + series
    
    results = [item for item in combined if q_lower in item['title'].lower() or q_lower in item['slug'].lower()]
    return {'query': query, 'total': len(results), 'data': results}

def get_movie_detail(slug):
    html = fetch_html(f'/movie/{slug}')
    
    # Try parsing JSON-LD schema
    schema_matches = re.findall(r'<script type="application/ld\+json">(.*?)</script>', html, re.DOTALL)
    metadata = {}
    for s in schema_matches:
        try:
            data = json.loads(s)
            if isinstance(data, list):
                for d in data:
                    if d.get('@type') == 'Movie': metadata = d
            elif data.get('@type') == 'Movie':
                metadata = data
        except Exception:
            pass
            
    title = metadata.get('name') or slug.replace('-', ' ').title()
    synopsis = metadata.get('description') or ''
    images = metadata.get('image', [])
    poster = images[0] if isinstance(images, list) and len(images) > 0 else f'https://image.tmdb.org/t/p/w500/{slug}.jpg'
    backdrop = images[1] if isinstance(images, list) and len(images) > 1 else poster
    
    genres = metadata.get('genre', ['Action', 'Drama'])
    if isinstance(genres, str): genres = [genres]
    
    director_data = metadata.get('director', {})
    director = director_data.get('name', 'Director') if isinstance(director_data, dict) else 'Director'
    
    actors = metadata.get('actor', [])
    cast = [a.get('name') for a in actors if isinstance(a, dict) and 'name' in a]
    
    servers = [
        {'name': 'Server 1 (IDLIX Stream)', 'url': f'https://z2.idlixku.com/movie/{slug}?play=1', 'quality': '1080p HD'},
        {'name': 'Server 2 (VidSrc HD)', 'url': f'https://vidsrc.me/embed/movie?imdb={slug}', 'quality': '1080p'},
        {'name': 'Server 3 (AutoEmbed)', 'url': f'https://autoembed.co/movie/{slug}', 'quality': '720p HD'},
        {'name': 'Server 4 (SmashyStream)', 'url': f'https://player.smashystream.com/movie/{slug}', 'quality': 'HD Multi-sub'}
    ]
    
    return {
        'id': slug,
        'slug': slug,
        'title': clean_text(title),
        'type': 'movie',
        'synopsis': clean_text(synopsis),
        'poster': poster,
        'backdrop': backdrop,
        'year': slug.split('-')[-1] if slug.split('-')[-1].isdigit() else '2024',
        'duration': '115 Menit',
        'rating': '8.3',
        'genres': genres,
        'director': director,
        'cast': cast if cast else ['Vincent Zhao', 'Michael Tong'],
        'servers': servers
    }

def get_series_detail(slug):
    html = fetch_html(f'/series/{slug}')
    episodes = re.findall(r'href="((?:/series/|/episode/)[^"]+)"', html)
    
    ep_list = []
    seen = set()
    for link in episodes:
        if '/season/' in link and link not in seen:
            seen.add(link)
            season_m = re.search(r'season/(\d+)', link)
            ep_m = re.search(r'episode/(\d+)', link)
            s_num = int(season_m.group(1)) if season_m else 1
            e_num = int(ep_m.group(1)) if ep_m else 1
            
            ep_list.append({
                'season': s_num,
                'episode': e_num,
                'title': f'Episode {e_num}',
                'link': link,
                'streamUrl': f'https://z2.idlixku.com{link}?play=1'
            })
            
    # Group by season
    seasons_dict = {}
    for ep in ep_list:
        s = ep['season']
        if s not in seasons_dict: seasons_dict[s] = []
        seasons_dict[s].append(ep)
        
    seasons = []
    for s_num in sorted(seasons_dict.keys()):
        seasons.append({
            'seasonNumber': s_num,
            'episodes': sorted(seasons_dict[s_num], key=lambda x: x['episode'])
        })
        
    if not seasons:
        seasons = [{
            'seasonNumber': 1,
            'episodes': [
                {'season': 1, 'episode': 1, 'title': 'Episode 1', 'link': f'/series/{slug}/season/1/episode/1', 'streamUrl': f'https://z2.idlixku.com/series/{slug}/season/1/episode/1?play=1'},
                {'season': 1, 'episode': 2, 'title': 'Episode 2', 'link': f'/series/{slug}/season/1/episode/2', 'streamUrl': f'https://z2.idlixku.com/series/{slug}/season/1/episode/2?play=1'}
            ]
        }]
        
    return {
        'id': slug,
        'slug': slug,
        'title': slug.replace('-', ' ').title(),
        'type': 'series',
        'synopsis': f'Nonton Serial TV {slug} lengkap gratis di FilmIn.',
        'poster': f'https://image.tmdb.org/t/p/w500/{slug}.jpg',
        'backdrop': f'https://image.tmdb.org/t/p/w1280/{slug}.jpg',
        'rating': '8.7',
        'seasons': seasons
    }

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(json.dumps({'error': 'No command specified'}))
        sys.exit(1)
        
    cmd = sys.argv[1]
    param = sys.argv[2] if len(sys.argv) > 2 else ''
    
    try:
        if cmd == 'home':
            res = get_home()
        elif cmd == 'catalog':
            res = get_catalog(param or 'movie')
        elif cmd == 'genre':
            res = get_genre(param or 'action')
        elif cmd == 'search':
            res = search(param)
        elif cmd == 'movie-detail':
            res = get_movie_detail(param)
        elif cmd == 'series-detail':
            res = get_series_detail(param)
        else:
            res = {'error': f'Unknown command {cmd}'}
            
        print(json.dumps(res, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({'error': str(e)}))
        sys.exit(1)
