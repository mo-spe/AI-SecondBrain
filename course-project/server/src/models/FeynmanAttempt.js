import mongoose from 'mongoose';

const feynmanAttemptSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    knowledge: { type: mongoose.Schema.Types.ObjectId, ref: 'Knowledge', required: true, index: true },
    clientAttemptId: { type: String, maxlength: 100 },
    transcript: { type: String, required: true, maxlength: 30000 },
    polishedText: { type: String, required: true, maxlength: 30000 },
    score: { type: Number, required: true, min: 0, max: 100 },
    evaluation: { type: String, required: true, maxlength: 2000 },
    strengths: { type: [String], default: [] },
    weaknesses: { type: [String], default: [] }
  },
  { timestamps: true }
);

feynmanAttemptSchema.index({ user: 1, createdAt: -1 });
feynmanAttemptSchema.index({ user: 1, clientAttemptId: 1 }, { unique: true, sparse: true });

feynmanAttemptSchema.set('toJSON', {
  transform: (_document, returned) => {
    returned.id = returned._id.toString();
    returned.knowledgeId = returned.knowledge.toString();
    delete returned._id;
    delete returned.__v;
    delete returned.user;
    delete returned.knowledge;
    return returned;
  }
});

export const FeynmanAttempt = mongoose.model('FeynmanAttempt', feynmanAttemptSchema);
