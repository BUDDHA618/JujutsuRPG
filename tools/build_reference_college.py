"""Reference-inspired modular240x240 campus; outputs standalone vanilla NBT, no game dependencies."""
from pathlib import Path
import struct,gzip,json,zipfile,math,random,zlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/reference_college';OUT.mkdir(parents=True,exist_ok=True)
blocks={};modules=[];ALL={}
def put(x,y,z,name,**props):blocks[x,y,z]=('minecraft:'+name,tuple(sorted(props.items())))
def fill(a,b,name,**props):
 for x in range(a[0],b[0]+1):
  for y in range(a[1],b[1]+1):
   for z in range(a[2],b[2]+1):put(x,y,z,name,**props)
def air(a,b):
 for pos in list(blocks):
  if all(a[i]<=pos[i]<=b[i] for i in range(3)):del blocks[pos]
def string(s):
 b=s.encode();return struct.pack('>H',len(b))+b
def payload(t,v):
 if t==3:return struct.pack('>i',v)
 if t==8:return string(v)
 if t==9:
  kind,values=v;return bytes([kind])+struct.pack('>i',len(values))+b''.join(payload(kind,x) for x in values)
 if t==10:return b''.join(bytes([tt])+string(k)+payload(tt,vv) for k,(tt,vv) in v.items())+b'\0'
 raise ValueError(t)
def save(name):
 global blocks
 palette=[];ids={};entries=[]
 for pos,state in sorted(blocks.items()):
  assert all(0<=pos[i]<(240,64,240)[i] for i in range(3)),pos
  if state not in ids:
   ids[state]=len(palette);p={'Name':(8,state[0])}
   if state[1]:p['Properties']=(10,{k:(8,v) for k,v in state[1]})
   palette.append(p)
  entries.append({'pos':(9,(3,list(pos))),'state':(3,ids[state])})
 assert len(entries)<=200000,(name,len(entries))
 root={'DataVersion':(3,3465),'size':(9,(3,[240,64,240])),'palette':(9,(10,palette)),'blocks':(9,(10,entries)),'entities':(9,(10,[]))}
 (OUT/(name+'.nbt')).write_bytes(gzip.compress(b'\x0a\0\0'+payload(10,root),mtime=0))
 modules.append({'file':name+'.nbt','blocks':len(entries),'states':len(palette)})
 ALL.update(blocks);blocks={}
def lamp(x,y,z):put(x,y,z,'lantern',hanging='false',waterlogged='false')
def roof(x,z,w,d,y):
 for t in range(5):fill((x-2+t,y+t,z-2+t),(x+w+2-t,y+t,z+d+2-t),'deepslate_tiles')
 fill((x+3,y+5,z+d//2),(x+w-3,y+5,z+d//2),'polished_blackstone_bricks')
def house(x,z,w,d,floors,style):
 fill((x,2,z),(x+w,3,z+d),'stone_bricks')
 for floor in range(floors):
  y=3+floor*6
  fill((x,y,z),(x+w,y,z+d),'polished_andesite' if floor==0 else 'dark_oak_planks')
  for zz in [z,z+d]:fill((x,y+1,zz),(x+w,y+5,zz),'smooth_quartz')
  for xx in [x,x+w]:fill((xx,y+1,z),(xx,y+5,z+d),'smooth_quartz')
  for xx in range(x,x+w+1,6):
   for zz in [z,z+d]:
    fill((xx,y+1,zz),(xx,y+5,zz),'stone_bricks')
    if xx+4<x+w:fill((xx+2,y+2,zz),(xx+4,y+4,zz),'glass')
  for zz in range(z+3,z+d-2,6):
   for xx in [x,x+w]:fill((xx,y+2,zz),(xx,y+4,zz+2),'glass')
  # Wide central east-west hall; rooms flank it, each with open two-block door.
  middle=z+d//2
  for zz in [middle-3,middle+3]:fill((x+1,y+1,zz),(x+w-1,y+4,zz),'white_terracotta')
  for xx in range(x+12,x+w-3,12):
   for z1,z2 in [(z+1,middle-4),(middle+4,z+d-1)]:fill((xx,y+1,z1),(xx,y+4,z2),'white_terracotta')
  for xx in range(x+5,x+w-5,12):
   for zz in [middle-3,middle+3]:air((xx,y+1,zz),(xx+1,y+3,zz))
  # Lighting in every classroom/bedroom and corridor, no block-entity data.
  for xx in range(x+4,x+w-2,8):
   for zz in [z+4,middle,z+d-4]:put(xx,y+5,zz,'sea_lantern')
  # Room furniture per intended function.
  for xx in range(x+3,x+w-7,12):
   for zz in [z+3,z+d-7]:
    if style=='dorm':
     for bedx in [xx,xx+4]:
      fill((bedx,y+1,zz),(bedx+1,y+1,zz+3),'blue_wool');fill((bedx,y+2,zz),(bedx+1,y+2,zz),'white_carpet');put(bedx,y+1,zz+4,'oak_planks');lamp(bedx,y+2,zz+4)
    elif style=='library':
     fill((xx,y+1,zz),(xx+5,y+3,zz),'bookshelf');fill((xx,y+1,zz+3),(xx+3,y+1,zz+3),'dark_oak_planks');lamp(xx+1,y+2,zz+3)
    elif style=='lab':
     fill((xx,y+1,zz),(xx+6,y+1,zz),'smooth_quartz');put(xx+1,y+2,zz,'brewing_stand');put(xx+4,y+2,zz,'amethyst_cluster',facing='up',waterlogged='false');put(xx+6,y+1,zz+2,'cauldron')
    elif style=='med':
     fill((xx,y+1,zz),(xx+1,y+1,zz+3),'white_wool');put(xx+3,y+1,zz,'iron_block');lamp(xx+3,y+2,zz)
    else:
     for row in [0,3]:
      fill((xx,y+1,zz+row),(xx+3,y+1,zz+row),'oak_planks')
      for seat in [xx,xx+3]:put(seat,y+1,zz+row+1,'oak_stairs',facing='north',half='bottom',shape='straight',waterlogged='false')
  # Front door on each floor; upper doors become balcony thresholds.
  mid=x+w//2;air((mid-2,y+1,z),(mid+2,y+3,z))
  # True walkable staircase: six rising steps per storey inside the central hall.
  if floor<floors-1:
   for step in range(6):
    sx=x+w-10+step
    fill((sx,y+1,middle-1),(sx,y+step,middle+1),'stone_bricks') if step else None
    fill((sx,y+step+1,middle-1),(sx,y+step+1,middle+1),'stone_brick_stairs',facing='east',half='bottom',shape='straight',waterlogged='false')
 # Cut stairwell openings through next floor without deleting treads; keep end landing.
 for floor in range(1,floors):
  y=3+floor*6;air((x+w-10,y,z+d//2-1),(x+w-6,y,z+d//2+1))
 roof(x,z,w,d,3+floors*6)
 # Buttresses and ornamental trim.
 for xx in range(x,x+w+1,6):
  for zz in [z-1,z+d+1]:fill((xx,3,zz),(xx,3+floors*6,zz),'stone_bricks')
# 01 landscape / roads / walls. Sparse low platform, not a solid terrain cube.
fill((0,0,0),(239,0,239),'stone');fill((0,1,0),(239,1,239),'dirt');fill((0,2,0),(239,2,239),'grass_block',snowy='false')
for z in [0,239]:fill((0,3,z),(239,7,z),'stone_bricks')
for x in [0,239]:fill((x,3,0),(x,7,239),'stone_bricks')
for x in range(0,240,12):
 for z in [0,239]:fill((x,8,z),(min(239,x+2),8,z),'stone_bricks')
for x1,z1,x2,z2 in [(106,0,113,195),(5,106,230,113),(5,181,230,187),(5,72,230,77),(65,10,71,181),(145,10,151,230)]:fill((x1,2,z1),(x2,2,z2),'stone_bricks')
# Gate openings are omitted, not air blocks.
air((103,3,0),(116,8,0))
for x in [101,116]:fill((x,3,0),(x+2,16,3),'stone_bricks')
fill((99,15,0),(120,16,4),'dark_oak_planks');roof(101,3,17,4,17)
# Road lighting.
for z in range(10,195,14):
 for x in [104,115]:fill((x,3,z),(x,5,z),'stone_bricks');lamp(x,6,z)
# Woodland perimeter; leave building footprints clear.
rng=random.Random(618)
for x,z in [(x,z) for x in range(8,235,9) for z in [7,232]]+[(x,z) for z in range(10,235,10) for x in [7,232]]:
 h=rng.randint(5,8);fill((x,3,z),(x,3+h,z),'oak_log',axis='y');fill((x-2,3+h,z-2),(x+2,5+h,z+2),'oak_leaves',persistent='true',distance='1',waterlogged='false')
# Pond: water enclosed by continuous rim.
fill((171,1,16),(218,2,43),'stone_bricks');fill((172,2,17),(217,2,42),'water',level='0')
for x in [184,185,186]:fill((x,3,15),(x,3,44),'dark_oak_planks')
# Pause island and garden lanterns.
fill((198,2,27),(205,3,33),'moss_block');lamp(201,4,30)
save('01_territory')
# 02 main building four levels + central tower, furnished classrooms/admin.
house(78,124,62,36,4,'class')
fill((102,3,120),(116,36,124),'stone_bricks');air((105,4,120),(113,7,124))
for y in [12,19,26,33]:fill((106,y,119),(112,y+2,119),'glass')
roof(102,120,14,5,37);save('02_main_four_floors')
# 03 two four-storey dorms.
house(15,117,46,24,4,'dorm');house(15,151,46,24,4,'dorm');save('03_dormitories')
# 04 library and research wing.
house(165,116,44,24,3,'library');house(162,151,47,25,3,'lab');save('04_library_laboratories')
# 05 dining room and infirmary.
house(81,82,46,19,2,'class');house(173,52,44,19,2,'med')
for x in [88,101,114]:
 for z in [86,94]:fill((x,4,z),(x+6,4,z+1),'dark_oak_planks');lamp(x+3,5,z)
save('05_dining_medical')
# 06 sport complex: track, field, pool, courts, gym.
fill((14,2,17),(61,2,65),'red_concrete');fill((18,2,21),(57,2,61),'green_concrete')
for x in [18,57]:fill((x,3,21),(x,3,61),'white_carpet')
for z in [21,41,61]:fill((18,3,z),(57,3,z),'white_carpet')
for z in [20,62]:
 for x in [33,42]:fill((x,3,z),(x,7,z),'quartz_pillar',axis='y')
 fill((33,7,z),(42,7,z),'quartz_pillar',axis='x')
fill((15,1,84),(59,2,101),'smooth_quartz');fill((17,2,86),(57,2,99),'water',level='0')
for x in [27,37,47]:fill((x,1,86),(x,1,99),'blue_concrete')
for x1,x2 in [(76,96),(119,139)]:
 fill((x1,2,20),(x2,2,61),'red_concrete');fill((x1+1,3,40),(x2-1,3,40),'white_carpet')
 for x in [x1,x2]:fill((x,3,39),(x,5,39),'oak_fence',east='false',west='false',north='false',south='false',waterlogged='false')
house(79,189,50,27,2,'class');save('06_sports')
# 07 temple, training arena and ritual hall.
house(97,210,38,20,1,'library')
for tier in range(3):roof(100+tier*3,212+tier*2,32-tier*6,16-tier*4,15+tier*7)
fill((166,2,190),(218,2,225),'sandstone');fill((169,2,193),(215,2,222),'sand')
for x in range(173,216,8):
 fill((x,3,217),(x,5,217),'oak_fence',east='false',west='false',north='false',south='false',waterlogged='false');put(x,6,217,'target',power='0')
# Ritual circle built from purple/amethyst, not destructive effects.
for degree in range(0,360,3):
 a=math.radians(degree);put(192+round(math.cos(a)*10),2,205+round(math.sin(a)*10),'amethyst_block')
put(192,2,205,'sea_lantern');save('07_temple_training')
# Build top-down PNG from actual generated topmost blocks.
colors={'stone':(82,86,94),'stone_bricks':(141,145,151),'grass_block':(75,117,68),'water':(52,117,170),'deepslate_tiles':(40,52,67),'dark_oak_planks':(82,62,45),'smooth_quartz':(219,209,183),'sand':(197,176,132),'red_concrete':(157,69,62),'green_concrete':(57,126,65),'oak_leaves':(38,88,52)}
top={}
for (x,y,z),(name,props) in ALL.items():
 if (x,z) not in top or y>top[x,z][0]:top[x,z]=(y,colors.get(name.split(':')[1],(155,148,128)))
pixels=[]
for z in reversed(range(240)):
 row=[]
 for x in range(240):row.extend(top.get((x,z),(0,(25,30,40)))[1])
 pixels.append(b'\0'+bytes(row))
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
(OUT/'campus_map.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',240,240,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(pixels)))+chunk(b'IEND',b''))
(OUT/'manifest.json').write_text(json.dumps({'size':[240,64,240],'shared_origin':True,'modules':modules},indent=2))
(OUT/'ПРОЧИТАЙ.md').write_text('''# Техникум по референсам — 240×240

Это блочная адаптация, не точная копия изображений.
Все семь NBT используют ОДИН угол, ОДНУ высоту и ОДИН поворот.
Скопируй .nbt в config/jujutsurpg/buildings, затем обнови библиотеку.
Порядок:01 территория,02 главный,03 общежития,04 библиотека/лаборатории,
05 столовая/медицинский,06 спорт,07 храм/полигон.

Рекомендуется новый суперплоский тестовый мир. Строитель не очищает воздух:
деревья и грунт внутри зданий автоматически не удалятся.
Если поверхность мира на Y=-61, укажи общую точку Y=-63: газон схемы лежит на +2.
Игроку нужно находиться около центра комплекса (угол X+120,Z+120),
чтобы все блоки вошли в лимит256 от игрока. При необходимости перемещайся к центру.
Поворот и координаты НЕ меняй между модулями. Для новой постройки сначала предпросмотр.
Все нужные чанки должны быть загружены: далеко незагруженные части будут пропущены.
Отмена только одного последнего модуля до выхода. Для всего комплекса используй копию мира.

Интерьеры:коридоры,классы,комнаты общежития,библиотечные полки,
лабораторные столы,медицинские кушетки,лестницы,светильники.
Кровати представлены декоративными блоками (не работают как точка возрождения).
Строитель пропускает NPC и блоки с данными, включая варочные стойки.
Новых NPC этот набор не добавляет. Студентов можно призвать яйцами.

Постройки ещё не проверены визуально и на проходимость в игре.
На карте PNG северная часть комплекса (храм) сверху, главные ворота снизу.
''',encoding='utf-8')
with zipfile.ZipFile(ROOT/'output/reference_college.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for p in OUT.iterdir():archive.write(p,p.name)
print(json.dumps(modules,indent=2))

