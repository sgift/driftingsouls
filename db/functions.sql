-- Berechnet die naechste freie Schiffs-ID ab der angegebenen ID
DROP FUNCTION IF EXISTS newIntelliShipID;
DELIMITER //
CREATE FUNCTION newIntelliShipID(minid INT) RETURNS INT
READS SQL DATA
BEGIN
  DECLARE done,sid,shouldId INT DEFAULT 0;
  DECLARE cur1 CURSOR FOR SELECT DISTINCT abs(id) iid FROM ships WHERE abs(id)>=minid ORDER BY iid;
  DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;

  OPEN cur1;
  set shouldId = minid;
  REPEAT
    FETCH cur1 INTO sid;
    IF NOT done THEN
      IF sid <> shouldId THEN
        CLOSE cur1;
        RETURN shouldId;
      END IF;
      set shouldId = shouldId+1;
    END IF;
  UNTIL done END REPEAT;

  CLOSE cur1;
  RETURN shouldId;
END;
//