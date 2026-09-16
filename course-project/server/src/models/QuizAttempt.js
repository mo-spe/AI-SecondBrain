import mongoose from 'mongoose';

const quizAttemptSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    knowledge: { type: mongoose.Schema.Types.ObjectId, ref: 'Knowledge', required: true, index: true },
    difficulty: { type: String, enum: ['easy', 'medium', 'hard'], required: true },
    questionSnapshot: {
      question: { type: String, required: true, maxlength: 500 },
      options: { type: [String], required: true, validate: (options) => options.length === 4 },
      correctIndex: { type: Number, required: true, min: 0, max: 3 }
    },
    userAnswer: { type: Number, required: true, min: 0, max: 3 },
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
