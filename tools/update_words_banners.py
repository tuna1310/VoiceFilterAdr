import json
from pathlib import Path
p=Path('app/src/main/assets/words.json')
data=json.loads(p.read_text(encoding='utf-8'))
map={
 'colors_b_008':'file:///android_asset/banners/banner_colors_brown.webp',
 'colors_e_031':'file:///android_asset/banners/banner_colors_turquoise.webp',
 'car_brands_b_046':'file:///android_asset/banners/banner_car_brands_ford.webp',
 'car_brands_b_052':'file:///android_asset/banners/banner_car_brands_nissan.webp',
 'animals_b_185':'file:///android_asset/banners/banner_animals_lion.webp',
 'animals_b_186':'file:///android_asset/banners/banner_animals_tiger.webp',
}
for w in data:
    if w.get('id') == 'colors_b_009':
        w.setdefault('accepted_variants', ['grey'])
    if w.get('id') in map:
        w['banner']=map[w['id']]
p.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n", encoding='utf-8')
print('updated', sum(1 for w in data if 'banner' in w))
print([(w['id'],w['word'],w.get('banner')) for w in data if w.get('id') in map])
