-- @author bwcx_jzy

ALTER TABLE USEROPERATELOGV1
	ALTER COLUMN nodeId VARCHAR(50) COMMENT '节点ID';

ALTER TABLE MONITORNOTIFYLOG
	ALTER COLUMN nodeId VARCHAR(50) COMMENT '节点ID';

ALTER TABLE OUTGIVINGLOG
	ADD IF NOT EXISTS modifyUser VARCHAR(50) comment '操作人';

ALTER TABLE SSHTERMINALEXECUTELOG
	ADD IF NOT EXISTS modifyUser VARCHAR(50) comment '操作人';

ALTER TABLE BUILDHISTORYLOG
	ADD IF NOT EXISTS triggerBuildType int DEFAULT 0 comment '触发类型{0，手动，1 触发器,2 自动触发}';

ALTER TABLE BUILDHISTORYLOG
	ADD IF NOT EXISTS diffSync TINYINT COMMENT '增量同步';


ALTER TABLE SSHTERMINALEXECUTELOG
	ALTER COLUMN commands CLOB comment '操作的命令';

ALTER TABLE SYSTEMMONITORLOG
	ADD IF NOT EXISTS networkTime int COMMENT '延迟时间ms';
