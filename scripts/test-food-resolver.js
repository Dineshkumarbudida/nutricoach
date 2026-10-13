const assert = (condition, message) => { if (!condition) throw new Error(message); };
const aliases = {
  dal: ["lentils","dhal","daal","pulses"],
  toor: ["toor dal","tuvar dal","arhar dal","tur dal","pigeon pea"],
  chana: ["chickpea","chickpeas","garbanzo"],
  curd: ["dahi","yogurt"],
  paneer: ["cottage cheese","indian cottage cheese"],
  bhindi: ["okra","lady finger"],
  baingan: ["eggplant","brinjal","aubergine"],
  palak: ["spinach"],
  anda: ["egg","eggs"],
  chawal: ["rice"],
  ragi: ["finger millet","nachni"],
  moong: ["moong dal","mung dal","green gram"],
  urad: ["urad dal","black gram"],
  rajma: ["kidney beans","red kidney beans"]
};
const normalize = (text) => String(text || "").toLowerCase().replace(/[^a-z0-9]+/g," ").trim().replace(/\s+/g," ");
function terms(name, regional="") {
  const base = normalize(name), out = new Set([base]);
  const regionalNorm = normalize(regional);
  if (regionalNorm) out.add(regionalNorm);
  Object.entries(aliases).forEach(([key, values]) => {
    const k=normalize(key), vals=values.map(normalize);
    if (base===k || base.includes(k) || vals.some(v=>base.includes(v) || v.includes(base))) {
      out.add(k); vals.forEach(v=>out.add(v));
    }
  });
  return out;
}
assert(terms("Pigeon pea, split").has("toor"), "toor -> pigeon pea");
assert(terms("Red lentils").has("dal"), "dal -> lentils");
assert(terms("Spinach","Palak").has("palak"), "palak regional alias");
assert(terms("Kidney beans").has("rajma"), "rajma -> kidney beans");
console.log("Food resolver tests passed");
