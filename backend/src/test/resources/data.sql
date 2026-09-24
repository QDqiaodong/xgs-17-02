INSERT INTO wq_threshold
(metric_code, metric_name, unit, judge_type, min_value, max_value, pass_values, display_order, enabled)
SELECT 'residual_chlorine', '余氯', 'mg/L', 'range', 0.030, 0.800, '', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM wq_threshold WHERE metric_code = 'residual_chlorine');

INSERT INTO wq_threshold
(metric_code, metric_name, unit, judge_type, min_value, max_value, pass_values, display_order, enabled)
SELECT 'turbidity', '浊度', 'NTU', 'range', 0.000, 1.000, '', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM wq_threshold WHERE metric_code = 'turbidity');

INSERT INTO wq_threshold
(metric_code, metric_name, unit, judge_type, min_value, max_value, pass_values, display_order, enabled)
SELECT 'temperature', '温度', '℃', 'range', 0.000, 40.000, '', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM wq_threshold WHERE metric_code = 'temperature');

INSERT INTO wq_threshold
(metric_code, metric_name, unit, judge_type, min_value, max_value, pass_values, display_order, enabled)
SELECT 'odor', '气味', '', 'enum', NULL, NULL, '无异味,正常', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM wq_threshold WHERE metric_code = 'odor');
