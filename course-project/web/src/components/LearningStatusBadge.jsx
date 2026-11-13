export default function LearningStatusBadge({ knowledge }) {
  const learned = typeof knowledge?.reviewScore === 'number';
  const mastered = learned && knowledge.reviewScore >= 60;
  return <span className={`learning-status ${mastered ? 'mastered' : learned ? 'learning' : 'not-learned'}`}>{!learned ? '未学习' : mastered ? '已学习且已掌握' : '已学习但未掌握'}</span>;
}
