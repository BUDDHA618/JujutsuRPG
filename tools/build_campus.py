"""Reproducible campus structure, vanilla NBT and datapack world generation. No dependencies."""
from pathlib import Path
import struct,gzip,json,zlib
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/jujutsurpg'
def js(path,obj):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
blocks={}
def put(x,y,z,name,**props):blocks[x,y,z]=(name,tuple(sorted(props.items())))
def fill(x1,y1,z1,x2,y2,z2,name,**props):
 for x in range(x1,x2+1):
  for y in range(y1,y2+1):
   for z in range(z1,z2+1):put(x,y,z,name,**props)
# Flat terrace; buried foundation reduces exposed edges on gentle terrain.
fill(0,0,0,111,5,111,'minecraft:stone_bricks')
fill(1,6,1,110,6,110,'minecraft:grass_block',snowy='false')
# Clear campus volume, including vegetation, before erecting buildings.
fill(0,7,0,111,26,111,'minecraft:air')
for x1,z1,x2,z2 in [(51,0,60,100),(10,47,101,54),(12,90,100,97)]:fill(x1,6,z1,x2,6,z2,'minecraft:polished_andesite')
# Perimeter, gap at south gate.
for z in [0,111]:fill(0,7,z,111,9,z,'minecraft:stone_brick_wall',east='low',west='low',north='none',south='none',up='true',waterlogged='false')
for x in [0,111]:fill(x,7,0,x,9,111,'minecraft:stone_bricks')
fill(50,7,0,61,10,0,'minecraft:air')
# Torii gate and lantern approach.
for x in [49,62]:fill(x,7,1,x+1,17,2,'minecraft:dark_oak_log',axis='y')
fill(46,16,0,65,17,3,'minecraft:red_terracotta')
fill(45,18,0,66,18,3,'minecraft:dark_oak_planks')
for z in range(10,90,12):
 for x in [48,63]:
  fill(x,7,z,x,9,z,'minecraft:stone_bricks');put(x,10,z,'minecraft:lantern',hanging='false',waterlogged='false')
def building(x,z,w,d):
 fill(x,6,z,x+w,7,z+d,'minecraft:stone_bricks')
 fill(x+1,8,z+1,x+w-1,8,z+d-1,'minecraft:dark_oak_planks')
 fill(x,9,z,x+w,14,z+d,'minecraft:white_terracotta')
 fill(x+1,9,z+1,x+w-1,14,z+d-1,'minecraft:air')
 for xx in [x,x+w]:
  for zz in [z,z+d]:fill(xx,8,zz,xx,15,zz,'minecraft:dark_oak_log',axis='y')
 # Window bands, two floors' visual trim.
 for xx in range(x+3,x+w-2,5):
  for zz in [z,z+d]:fill(xx,10,zz,xx+2,12,zz,'minecraft:glass')
 for zz in range(z+3,z+d-2,5):
  for xx in [x,x+w]:fill(xx,10,zz,xx,12,zz+2,'minecraft:glass')
 fill(x-1,15,z-1,x+w+1,15,z+d+1,'minecraft:dark_oak_planks')
 # Stepped broad roof, dark temple silhouette.
 for tier in range(5):fill(x-2+tier,16+tier,z-2+tier,x+w+2-tier,16+tier,z+d+2-tier,'minecraft:deepslate_tiles')
 mid=x+w//2
 fill(mid-1,9,z,mid+1,11,z,'minecraft:air')
 fill(mid-1,7,z-3,mid+1,7,z-1,'minecraft:stone_bricks')
 fill(mid-1,8,z-1,mid+1,8,z-1,'minecraft:stone_bricks')
 for xx in [x+3,x+w-3]:
  for zz in [z+3,z+d-3]:put(xx,14,zz,'minecraft:lantern',hanging='true',waterlogged='false')
 return mid
building(30,72,51,29) # main school
building(7,21,30,20) # classroom
building(75,21,29,20) # dorm
building(7,63,17,30) # library
# Main hall desks and lectern.
for x in range(36,79,8):
 for z in [78,85,92]:
  fill(x,9,z,x+2,9,z,'minecraft:oak_planks');put(x+1,9,z+1,'minecraft:oak_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
put(55,9,97,'minecraft:lectern',facing='south',has_book='false',powered='false')
# Beds in dorm, bookshelves in library.
for x in [79,85,91,97]:
 for z in [26,33]:
  put(x,9,z,'minecraft:blue_bed',facing='north',part='foot',occupied='false');put(x,9,z-1,'minecraft:blue_bed',facing='north',part='head',occupied='false')
for z in range(67,90):
 for x in [9,21]:fill(x,9,z,x,12,z,'minecraft:bookshelf')
# Training ground: sand arena, target walls and wooden dummies.
fill(76,6,61,104,6,92,'minecraft:sand')
for x in [80,88,96]:
 fill(x,7,86,x,9,86,'minecraft:oak_fence',east='false',west='false',north='false',south='false',waterlogged='false')
 put(x,10,86,'minecraft:target',power='0')
# Central garden and shallow pool.
fill(39,6,31,47,6,39,'minecraft:stone_bricks');fill(40,6,32,46,6,38,'minecraft:water',level='0')
for x,z in [(15,50),(96,50),(39,61),(68,62)]:
 fill(x,7,z,x,12,z,'minecraft:oak_log',axis='y')
 fill(x-2,11,z-2,x+2,13,z+2,'minecraft:oak_leaves',distance='1',persistent='true',waterlogged='false')
# Tiny dependency-free NBT encoder.
def string(s):
 b=s.encode('utf-8');return struct.pack('>H',len(b))+b
def payload(t,v):
 if t==1:return struct.pack('>b',v)
 if t==3:return struct.pack('>i',v)
 if t==6:return struct.pack('>d',v)
 if t==8:return string(v)
 if t==9:
  subtype,values=v;return bytes([subtype])+struct.pack('>i',len(values))+b''.join(payload(subtype,x) for x in values)
 if t==10:return b''.join(bytes([tt])+string(k)+payload(tt,vv) for k,(tt,vv) in v.items())+b'\0'
 raise ValueError(t)
palette=[];indices={};entries=[]
for (x,y,z),(name,props) in sorted(blocks.items()):
 key=(name,props)
 if key not in indices:
  indices[key]=len(palette);state={'Name':(8,name)}
  if props:state['Properties']=(10,{k:(8,v) for k,v in props})
  palette.append(state)
 entries.append({'pos':(9,(3,[x,y,z])),'state':(3,indices[key])})
entities=[]
for i,(x,y,z) in enumerate([(54,7,14),(59,7,34),(50,7,52),(62,7,58),(82,7,68),(91,7,75),(15,9,30),(27,9,33),(82,9,35),(96,9,29),(40,9,81),(68,9,87)]):
 entities.append({'pos':(9,(6,[x+.5,float(y),z+.5])),'blockPos':(9,(3,[x,y,z])),
  'nbt':(10,{'id':(8,'jujutsurpg:student_mage'),'PersistenceRequired':(1,1),'CustomName':(8,json.dumps({'text':f'Студент {i+1}'},ensure_ascii=False))})})
root={'DataVersion':(3,3465),'size':(9,(3,[112,27,112])),'palette':(9,(10,palette)),'blocks':(9,(10,entries)),'entities':(9,(10,entities))}
p=DATA/'structures/technical_college/campus.nbt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(gzip.compress(b'\x0a\0\0'+payload(10,root),mtime=0))
js(Path('data/jujutsurpg/worldgen/template_pool/technical_college/start.json'),{'name':'jujutsurpg:technical_college/start','fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'jujutsurpg:technical_college/campus','processors':'minecraft:empty','projection':'rigid'}}]})
js(Path('data/jujutsurpg/worldgen/structure/technical_college.json'),{'type':'minecraft:jigsaw','biomes':'#jujutsurpg:has_technical_college','step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'jujutsurpg:technical_college/start','size':1,'start_height':{'absolute':-6},'project_start_to_heightmap':'WORLD_SURFACE_WG','max_distance_from_center':116,'use_expansion_hack':False})
js(Path('data/jujutsurpg/worldgen/structure_set/technical_colleges.json'),{'structures':[{'structure':'jujutsurpg:technical_college','weight':1}],'placement':{'type':'minecraft:random_spread','spacing':48,'separation':24,'salt':6180417}})
js(Path('data/jujutsurpg/tags/worldgen/biome/has_technical_college.json'),{'replace':False,'values':['minecraft:plains','minecraft:sunflower_plains','minecraft:forest','minecraft:birch_forest']})
js(Path('data/jujutsurpg/tags/worldgen/structure/technical_colleges.json'),{'replace':False,'values':['jujutsurpg:technical_college']})
js(Path('assets/jujutsurpg/models/item/student_spawn_egg.json'),{'parent':'minecraft:item/template_spawn_egg'})
for lang in ['ru_ru','en_us']:
 p=ROOT/f'assets/jujutsurpg/lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8-sig'));d['entity.jujutsurpg.student_mage']='Студент-маг' if lang=='ru_ru' else 'Student Mage';d['item.jujutsurpg.student_spawn_egg']='Яйцо призыва студента-мага' if lang=='ru_ru' else 'Student Mage Spawn Egg';p.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
# Vanilla player UV layout: dark uniform, face/hands, black hair, brass buttons.
pixels=[(22,31,51)]*4096
def rect(x1,y1,x2,y2,c):
 for yy in range(y1,y2):
  for xx in range(x1,x2):pixels[yy*64+xx]=c
rect(0,0,32,16,(24,20,29));rect(8,10,16,16,(211,173,138));rect(9,12,11,13,(35,31,38));rect(13,12,15,13,(35,31,38));rect(44,28,48,32,(211,173,138));rect(36,60,40,64,(211,173,138))
for y in [21,24,27]:rect(23,y,24,y+1,(223,180,71))
def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
def base(x,y):
 return (y<16 and x<32) or (16<=y<32 and x<56) or (48<=y<64 and 16<=x<48)
raw=b''.join(b'\0'+bytes(c for x in range(64) for c in (*pixels[y*64+x],255 if base(x,y) else 0)) for y in range(64))
p=ROOT/'assets/jujutsurpg/textures/entity/student.png';p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',64,64,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))
assert len(entities)==12 and max(x for x,y,z in blocks)==111
print(f'Campus112x112: {len(entries)} blocks, {len(palette)} states, 12 students; NBT {p.stat().st_size} texture bytes')
