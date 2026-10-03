from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
OUT = Path('app/src/main/assets/banners'); OUT.mkdir(parents=True, exist_ok=True)
W,H=600,800
try:
    FONT_BOLD=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',54); FONT_MED=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',34)
except Exception:
    FONT_BOLD=FONT_MED=ImageFont.load_default()
CASES=[('colors','brown','Brown','colors_b_008',(87,54,35),'paint'),('colors','turquoise','Turquoise','colors_i_017',(37,190,186),'paint'),('car_brands','ford','Ford','car_b_001',(50,88,180),'car'),('car_brands','nissan','Nissan','car_b_002',(220,80,70),'car'),('animals','lion','Lion','animals_b_001',(239,171,64),'animal'),('animals','fox','Fox','animals_b_002',(238,112,43),'animal')]
def bg(d):
    for y in range(H): d.line([(0,y),(W,y)], fill=(20,110+int(60*y/H),220))
    cx,cy=W*.62,H*.18
    for i in range(22):
        a=i*math.pi/11; d.line([(cx,cy),(cx+math.cos(a)*520,cy+math.sin(a)*520)], fill=(255,255,255,28), width=1)
    for x in range(25,W,38):
        for y in range(35,int(H*.55),38): d.ellipse((x,y,x+3,y+3), fill=(255,255,255,55))
    for i in range(8):
        y=80+i*38; d.line([(330,y),(430,y),(430,y+22),(520,y+22)], fill=(151,223,255,100), width=3)
def runway(d):
    p1=(-120,780); p2=(305,360); p3=(760,360); p4=(330,780)
    d.polygon([(p1[0],p1[1]+38),(p4[0],p4[1]+38),(p3[0],p3[1]+38),p3,p4,p1], fill=(50,68,82))
    d.polygon([p1,p2,p3,p4], fill=(124,228,46), outline=(235,255,205))
    d.line([p1,p2,p3,p4,p1], fill=(235,255,205), width=3)
    for i in range(16):
        t=i/15; lx=p1[0]+(p2[0]-p1[0])*t; ly=p1[1]+(p2[1]-p1[1])*t; rx=p4[0]+(p3[0]-p4[0])*t; ry=p4[1]+(p3[1]-p4[1])*t
        c=(220,30,45) if i%2==0 else (255,255,255); d.line([(lx,ly),(lx+25,ly-25)], fill=c, width=10); d.line([(rx,ry),(rx+25,ry-25)], fill=c, width=10)
def gate(d,word,accent,x=315,y=460):
    gw,gh=240,205; wood=(176,121,67); wood2=(219,164,94)
    for px in [x-gw//2,x+gw//2-48]:
        d.rounded_rectangle((px,y-gh+70,px+48,y+110), radius=10, fill=wood, outline=(110,70,35), width=3)
        for k in range(4): d.line((px+8+k*9,y-gh+80,px+14+k*9,y+100), fill=(236,184,118), width=2)
    d.rounded_rectangle((x-gw//2-18,y-gh+40,x+gw//2+18,y-gh+105), radius=16, fill=wood2, outline=(110,70,35), width=4)
    d.rounded_rectangle((x-gw//2+8,y-gh+50,x+gw//2-8,y-gh+95), radius=12, fill=accent, outline=(255,255,255), width=3)
    bb=d.textbbox((0,0), word.upper(), font=FONT_MED); d.text((x-(bb[2]-bb[0])/2,y-gh+56), word.upper(), fill=(255,255,255), font=FONT_MED)
    d.ellipse((x-48,y-72,x+48,y+24), outline=(230,235,238), width=15)
def fox(d,x=130,y=590):
    orange=(235,111,37); dark=(104,52,27); cream=(255,222,172)
    d.ellipse((x-45,y-60,x+48,y+36), fill=orange, outline=dark, width=4); d.polygon([(x-35,y-45),(x-65,y-95),(x-10,y-65)], fill=orange, outline=dark); d.polygon([(x+35,y-45),(x+65,y-95),(x+10,y-65)], fill=orange, outline=dark)
    d.ellipse((x-28,y-12,x+30,y+35), fill=cream); d.ellipse((x-20,y-25,x-10,y-15), fill=(25,25,25)); d.ellipse((x+12,y-25,x+22,y-15), fill=(25,25,25)); d.polygon([(x-5,y-5),(x+6,y-5),(x,y+3)], fill=(25,25,25))
    d.ellipse((x-28,y+35,x+30,y+100), fill=orange, outline=dark, width=4); d.line((x-8,y+90,x-25,y+135), fill=dark, width=9); d.line((x+16,y+90,x+35,y+132), fill=dark, width=9); d.ellipse((x+35,y+30,x+120,y+80), fill=orange, outline=dark, width=4); d.ellipse((x+94,y+35,x+132,y+74), fill=cream)
def obj(d,kind,word,accent):
    if kind=='paint':
        d.rounded_rectangle((390,535,520,665), radius=18, fill=accent, outline=(255,255,255), width=5); d.rectangle((412,500,498,548), fill=(230,230,230), outline=(80,80,80), width=4); d.ellipse((420,575,490,635), fill=(255,255,255,80), outline=(255,255,255), width=3)
    elif kind=='car':
        d.rounded_rectangle((350,565,545,650), radius=30, fill=accent, outline=(255,255,255), width=5); d.polygon([(390,565),(425,525),(500,525),(530,565)], fill=(180,225,255), outline=(255,255,255));
        for cx in (400,500): d.ellipse((cx-24,635,cx+24,683), fill=(25,25,30), outline=(220,220,220), width=5)
    elif word.lower()=='lion':
        d.ellipse((392,510,520,640), fill=(173,92,28), outline=(93,48,20), width=5); d.ellipse((418,535,495,610), fill=(239,171,64)); d.ellipse((432,555,442,565), fill=(20,20,20)); d.ellipse((472,555,482,565), fill=(20,20,20)); d.polygon([(455,575),(465,575),(460,583)], fill=(40,20,20))
    else: fox(d,455,570)
for cat,slug,word,wid,accent,kind in CASES:
    im=Image.new('RGB',(W,H)); d=ImageDraw.Draw(im,'RGBA'); bg(d); runway(d); gate(d,word,accent); fox(d); obj(d,kind,word,accent)
    d.rounded_rectangle((2,2,W-3,H-3), radius=46, outline=(255,255,255,180), width=6)
    out=OUT/f'banner_{cat}_{slug}.webp'; q=86
    while q>=45:
        im.save(out,'WEBP',quality=q,method=6)
        if out.stat().st_size<100000: break
        q-=7
    print(out, out.stat().st_size, q)
