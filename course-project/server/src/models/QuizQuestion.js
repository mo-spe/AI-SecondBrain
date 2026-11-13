import mongoose from 'mongoose';

const quizQuestionSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    knowledge: { type: mongoose.Schema.Types.ObjectId, ref: 'Knowledge', required: true, index: true },
    questionType: { type: String, enum: ['multiple-choice', 'short-answer'], default: 'multiple-choice' },
    difficulty: { type: String, enum: ['easy', 'medium', 'hard'], required: true },
    question: { type: String, required: true, maxlength: 500 },
    options: { type: [String], default: [], validate: function (options) { return this.questionType === 'short-answer' ? options.length === 0 : options.length === 4; } },
    correctIndex: { type: Number, required: function () { return this.questionType !== 'short-answer'; }, min: 0, max: 3 },
    referenceAnswer: { type: String, maxlength: 4000, required: function () { return this.questionType === 'short-answer'; } },
    keyPoints: { type: [String], default: [] },
    explanation: { type: String, required: true, maxlength: 2000 },
    provider: { type: String, required: true },
    expiresAt: { type: Date, required: true, default: () => new Date(Date.now() + 30 * 60 * 1000) }
  },
  { timestamps: true }
);

quizQuestionSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

export const QuizQuestion = mongoose.model('QuizQuestion', quizQuestionSchema);
