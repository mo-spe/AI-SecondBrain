import mongoose from 'mongoose';
import { createApp } from './app.js';
import { env } from './config/env.js';

const start = async () => {
  if (!env.mongodbUri) {
    throw new Error('MONGODB_URI is required. Configure the MongoDB Atlas connection string in server/.env.');
  }

  await mongoose.connect(env.mongodbUri, { dbName: 'feynman_course' });
  createApp().listen(env.port, () => {
    console.log(`Feynman course API listening on http://localhost:${env.port}`);
  });
};

start().catch((error) => {
  console.error('Failed to start course server', error);
  process.exitCode = 1;
});
