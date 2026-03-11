-- Update existing knowledge nodes to have nextReviewTime in the past (pending status)
UPDATE knowledge_node 
SET next_review_time = DATE_SUB(NOW(), INTERVAL 1 HOUR)
WHERE user_id = 5 
  AND next_review_time > NOW();
