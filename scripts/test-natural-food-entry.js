const assert=(condition,message)=>{if(!condition)throw new Error(message)};
const normalizeSearch=(text)=>String(text||'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim().replace(/\s+/g,' ');
function groupFoodEntryTokens(text){
  const normalized=String(text||'').trim();
  if(!normalized)return [];
  const chunks=normalized.replace(/[.!?]+/g,',').split(/,|\n|\s+and\s+/i).map(x=>x.trim()).filter(Boolean);
  return chunks.length?chunks:[normalizeSearch(normalized)];
}
assert(JSON.stringify(groupFoodEntryTokens('eggs, paneer, oats, rice, dal'))===JSON.stringify(['eggs','paneer','oats','rice','dal']),'comma input');
assert(JSON.stringify(groupFoodEntryTokens('I have eggs and paneer today'))===JSON.stringify(['I have eggs','paneer today']),'sentence split');
assert(groupFoodEntryTokens('soya chunks').length===1,'multiword food preserved');
console.log('Natural food entry parser tests passed');
