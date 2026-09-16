import mongoose from 'mongoose';

const quizQuestionSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    knowledge: { type: mongoose.Schema.Types.ObjectId, ref: 'Knowledge', required: true, index: true },
    difficulty: { type: String, enum: ['easy', 'medium', 'hard'], required: true },
    question: { type: String, required: true, maxlength: 500 },
    options: { type: [String], required: true, validate: (options) => options.length === 4 },
    correctIndex: { type: Number, required: true, min: 0, max: 3 },
    explanation: { type: String, required: true, maxlength: 2000 },
    provider: { type: String, required: true },
    expiresAt: { type: Date, required: true, default: () => new Date(Date.now() + 30 * 60 * 1000) }
  },
  { timestamps: true }
);

quizQuestionSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

export const QuizQuestion = mongoose.model('QuizQuestion', quizQuestionSchema);
