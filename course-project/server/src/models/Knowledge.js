import mongoose from 'mongoose';

const knowledgeSchema = new mongoose.Schema(
  {
    owner: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    title: { type: String, required: true, trim: true, maxlength: 120 },
    content: { type: String, required: true, maxlength: 30000 },
    tags: { type: [String], default: [] },
    reviewScore: { type: Number, default: null, min: 0, max: 100 },
    lastReviewedAt: { type: Date, default: null }
  },
  { timestamps: true }
);

knowledgeSchema.index({ owner: 1, updatedAt: -1 });

knowledgeSchema.set('toJSON', {
  transform: (_document, returned) => {
    returned.id = returned._id.toString();
    delete returned._id;
    delete returned.__v;
    delete returned.owner;
    return returned;
  }
});

export const Knowledge = mongoose.model('Knowledge', knowledgeSchema);

