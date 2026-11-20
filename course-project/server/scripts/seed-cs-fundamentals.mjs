import fs from 'node:fs';
import mongoose from 'mongoose';
import { env } from '../src/config/env.js';
import { User } from '../src/models/User.js';
import { Knowledge } from '../src/models/Knowledge.js';

const email = process.argv[2]?.trim().toLowerCase();
const items = JSON.parse(fs.readFileSync(new URL('../seeds/cs-fundamentals.json', import.meta.url), 'utf8'));

try {
  if (!email?.includes('@')) throw new Error('请传入现有账号邮箱');
  await mongoose.connect(env.mongodbUri, { dbName: 'feynman_course', serverSelectionTimeoutMS: 12000 });
  const user = await User.findOne({ email }).select('_id username');
  if (!user) throw new Error('目标账号不存在，不会自动创建账号');
  for (const item of items) await new Knowledge({ owner: user.id, ...item }).validate();
  const timestamp = new Date();
  let created = 0;
  for (const item of items) {
    // 关闭自动更新时间；同名记录重复导入时保留正文、掌握度和原有编辑时间。
    const result = await Knowledge.updateOne({ owner: user.id, title: item.title }, {
      $setOnInsert: { ...item, owner: user.id, reviewScore: null, lastReviewedAt: null, createdAt: timestamp, updatedAt: timestamp }
    }, { upsert: true, timestamps: false, setDefaultsOnInsert: true, runValidators: true });
    created += result.upsertedCount;
  }
  const saved = await Knowledge.find({ owner: user.id, title: { $in: items.map((item) => item.title) } });
  if (saved.length !== items.length) throw new Error('导入后数量校验失败，请检查记录');
  for (const item of saved) {
    const source = items.find((entry) => entry.title === item.title);
    if (created === items.length && (item.content !== source.content || item.reviewScore !== null)) throw new Error('新知识内容或初始状态校验失败');
  }
  console.log(JSON.stringify({ account: user.username, created, skipped: items.length - created, verifiedCount: saved.length, titles: saved.map((item) => item.title) }));
} catch (error) {
  console.error(`${error.name}: 导入未完成，请检查账号、数据库连接或数据校验`);
  process.exitCode = 1;
} finally {
  await mongoose.disconnect();
}
