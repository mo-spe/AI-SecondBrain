import { Knowledge } from '../models/Knowledge.js';

export async function buildKnowledgeGraph(userId) {
  const items = await Knowledge.find({ owner: userId }).sort({ updatedAt: -1 }).lean();
  const nodes = items.map((item) => ({
    id: item._id.toString(),
    label: item.title,
    excerpt: item.content.slice(0, 180),
    tags: item.tags,
    score: item.reviewScore
  }));
  const links = [];

  for (let leftIndex = 0; leftIndex < items.length; leftIndex += 1) {
    for (let rightIndex = leftIndex + 1; rightIndex < items.length; rightIndex += 1) {
      const leftTags = new Set(items[leftIndex].tags);
      const sharedTags = items[rightIndex].tags.filter((tag) => leftTags.has(tag));
      if (sharedTags.length === 0) continue;
      links.push({
        id: `${items[leftIndex]._id}-${items[rightIndex]._id}`,
        source: items[leftIndex]._id.toString(),
        target: items[rightIndex]._id.toString(),
        label: sharedTags.join(' · '),
        weight: sharedTags.length
      });
    }
  }

  return { nodes, links, stats: { nodeCount: nodes.length, linkCount: links.length, tagCount: new Set(items.flatMap((item) => item.tags)).size } };
}
