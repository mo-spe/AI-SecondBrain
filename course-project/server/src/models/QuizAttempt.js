import mongoose from 'mongoose';

const quizAttemptSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    knowledge: { type: mongoose.Schema.Types.ObjectId, ref: 'Knowledge', required: true, index: true },
    questionType: { type: String, enum: ['multiple-choice', 'short-answer'], default: 'multiple-choice' },
    difficulty: { type: String, enum: ['easy', 'medium', 'hard'], required: true },
    questionSnapshot: {
      question: { type: String, required: true, maxlength: 500 },
      options: { type: [String], default: [], validate: function (options) { return this.questionType === 'short-answer' ? options.length === 0 : options.length === 4; } },
      correctIndex: { type: Number, required: function () { return this.questionType !== 'short-answer'; }, min: 0, max: 3 },
      referenceAnswer: { type: String, maxlength: 4000 },
      keyPoints: { type: [String], default: [] }
    },
    userAnswer: { type: Number, required: function () { return this.questionType !== 'short-answer'; }, min: 0, max: 3 },
    userTextAnswer: { type: String, maxlength: 5000, required: function () { return this.questionType === 'short-answer'; } },
    score: { type: Number, min: 0, max: 100 },
    feedback: { type: String, maxlength: 2000 },
    provider: { type: String },
    isCorrect: { type: Boolean, required: true }
  },
  { timestamps: true }
);

quizAttemptSchema.index({ user: 1, createdAt: -1 });

quizAttemptSchema.set('toJSON', {
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

export const QuizAttempt = mongoose.model('QuizAttempt', quizAttemptSchema);
