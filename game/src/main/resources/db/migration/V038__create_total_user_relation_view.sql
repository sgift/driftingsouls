CREATE OR REPLACE VIEW v_total_user_relations AS
WITH T AS
(
	SELECT u.id, ur.status as defaultRelation, u2.id as target, CASE WHEN u.id=u2.id THEN 2 ELSE COALESCE(COALESCE(rur.status, ur.status), 0) END as relation_to FROM users u
		LEFT JOIN user_relations ur ON ur.user_id = u.id AND ur.target_id = 0
		CROSS JOIN users u2
		LEFT JOIN user_relations rur ON rur.user_id=u.id AND rur.target_id = u2.id
)
SELECT T.id, T.target, T.relation_to, T2.relation_to as relation_from FROM T
INNER JOIN T T2 on T.id = T2.target AND T.target = T2.id;