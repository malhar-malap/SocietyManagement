-- V15: Retire the accounts seeded by V2 that are still on their committed default credentials.
-- An account matches only if BOTH its email and its password hash are still exactly what V2 inserted, so the seeded
-- admin or any resident who has changed their password (or completed setup with a new email) is left untouched.
-- Covers admin@jaswanti.com and resident101..resident1008@jaswanti.com (81 accounts).
-- No rows are deleted, so rooms, bills, transactions and activity logs keep their references.
-- token_version is bumped so any token already issued to these accounts stops working immediately.
-- Runs on fresh databases and on existing Hibernate-built databases (baselined at 14).

-- Existing databases are baselined at 14, so V14 never runs there. If one predates the token_version column
-- (Hibernate only added it once the step 2 code had run against it), add it here before it is used.
SET @ddl = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0', 'DO 0')
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'token_version');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE users
SET is_active = 0,
    status = CASE WHEN status = 'BLOCKED' THEN 'BLOCKED' ELSE 'INACTIVE' END,
    token_version = token_version + 1
WHERE (email, password) IN (
    ('admin@jaswanti.com', '$2a$10$DAmzyQm0rGLPdbw9BZ70B.6qyhhTH5jQ9ez3ZAKaI6U.6ZhGnpKXO'),
    ('resident101@jaswanti.com', '$2a$10$9r75d7y/YRmqfiNFF4SFdOoHuknNcBEPme6HR5xbgvSJZhOdJ.lhq'),
    ('resident102@jaswanti.com', '$2a$10$kEhqjhyCxdihATo5KGeN0O7a1l2EtCcQMpXvSi9uygMBq23yB8kf2'),
    ('resident103@jaswanti.com', '$2a$10$t0tWIoRfcTBNuiU7cRjV5O9.7XVGwgXQO465nFso6nETZqFVbqPny'),
    ('resident104@jaswanti.com', '$2a$10$j.2q098WCzu6Paqn5hjQ1etDX5Rljh98N3ugEWO3e9CATNFlzNMMi'),
    ('resident105@jaswanti.com', '$2a$10$L4t4WY6Te8E.5.t7aBCMmuqqCzJlTzRxYboApEa8NRA7RzVcHNZIu'),
    ('resident106@jaswanti.com', '$2a$10$04DcUH8w408oeTNU5hdkpusUuGd1AIt5S.7WWTc74pgpYSZ2xSNqm'),
    ('resident107@jaswanti.com', '$2a$10$eME1bDJntrgYvNvAPofJzeSs/fOPaF42EroqxuOJc73UmeQUxK8Hm'),
    ('resident108@jaswanti.com', '$2a$10$k5blTG19IrEOxdbCqFhyq.g2vePFDiy3tBm/2e9n1mqp51mRMkX7C'),
    ('resident201@jaswanti.com', '$2a$10$tuA/0iE10.8/JmJdayoH.ey5U.DZJD3lLy9XD09U7wOs9T.ukzjs.'),
    ('resident202@jaswanti.com', '$2a$10$rxGO6iMsEMCi8Qjqsh47GOuwX10aIk.jqIEucJPY4BdKs3ggZkjgy'),
    ('resident203@jaswanti.com', '$2a$10$10jTLZtGPlkWf9u1GhGgMuzROgMfI54mQdzeG8bIS3/wrFCJ9N/Rq'),
    ('resident204@jaswanti.com', '$2a$10$HkpZ0Hi0Oe4PsCDSBQd6SuLgbhTY3kFs1yd9QSbaD6hgcG7jga1zy'),
    ('resident205@jaswanti.com', '$2a$10$VzVwM.U9cjRuFShqo3/h0eZr5fCwtC1yVjGA64XdiUFeJCKC9Fmw2'),
    ('resident206@jaswanti.com', '$2a$10$6mGXyJqTxJGJ2.B4HCDK6u.4g.qeg5nFYllHcQhiFm85eKlruAI4.'),
    ('resident207@jaswanti.com', '$2a$10$CkTf6E2SxGVwPZLlw1qVlOMfMZr5DnS5EszvaDqDRscrgNZCjwinK'),
    ('resident208@jaswanti.com', '$2a$10$x/Y7X50QKpNmqD39uSvL/uLCAfrRn3KrayAKShXo2yTkQLV8QhOMK'),
    ('resident301@jaswanti.com', '$2a$10$eKnQ3WAkDUtmipyKpQZ5NOO0BsCiZ6FlV7/ExMYNlNoOql.JH6Mey'),
    ('resident302@jaswanti.com', '$2a$10$OWlpk7.Uo18N9PChIV5Hp.8cGpLxYZWq6LiVmsOIUIwdssgW1M722'),
    ('resident303@jaswanti.com', '$2a$10$MFMsmI2UqgkzlE.APoTaButUAZBrPyHmzb3hMyKcz6iotPUe6g8em'),
    ('resident304@jaswanti.com', '$2a$10$CdbTZ3xI.dcmPBDFRCUdGer3Df59gFpPLM1us7m4LXIybKCOiPXku'),
    ('resident305@jaswanti.com', '$2a$10$4Ri/uQktWfqcd3TgAbsPvOC7oYkPHncdvUXCvIRfGuc.4uTl3eARm'),
    ('resident306@jaswanti.com', '$2a$10$TP9wHlkN/DeX6.PyR5qI3elzsoxkIh0qoaki3KRehp5cCkgahkKG2'),
    ('resident307@jaswanti.com', '$2a$10$PQy1tS8DGdXGb5gKiY2PWO9mebsspdjUux3lbAC5LMbNcQozllBYu'),
    ('resident308@jaswanti.com', '$2a$10$whGuWiOgj6CgGJE9ArAs..iFtixHr4JE7tKMS9b6nk2y3gzuixCbO'),
    ('resident401@jaswanti.com', '$2a$10$W3mpEH9fiXSNQQ3IOqVNsuasxDVVq0/Qy.3fSarEpf5DoGZBQgCxi'),
    ('resident402@jaswanti.com', '$2a$10$Ez/k67zKM8Sw.BzSNV3Wa.ixaP7GFJ7gzWzgfWsKJEasH/rxZ7kPO'),
    ('resident403@jaswanti.com', '$2a$10$25NbFDK6uKL8MS40HdivH.zg/DH4Ud87pwEdOYl2v0VZmESXbIa92'),
    ('resident404@jaswanti.com', '$2a$10$R3leyOGxeNMHzGtmeeajJuLWlLBEHqTwmizlrwQpPm191UiJop636'),
    ('resident405@jaswanti.com', '$2a$10$5b5ZGK1Uj3buTl5H/sIzrO1YFOays2ahLGZan1rq9aBQ2Rs8A2Gni'),
    ('resident406@jaswanti.com', '$2a$10$yjwptMqML/mgY7h7wR2a5uu4VBg261JVYxASjSFTuiKA/jcfnjaKq'),
    ('resident407@jaswanti.com', '$2a$10$BYzoAeiRCr3hj0NF3mReguTlf4B6t29rSkJ6qFKbupKysMCmNdVri'),
    ('resident408@jaswanti.com', '$2a$10$vwZbb26S6.eKLlw6/MSUmu5C0pZCsM8WdsKwG7rdTL7D1dPZpB5lS'),
    ('resident501@jaswanti.com', '$2a$10$6/4W6DK2kBb2cXbD.pC.VeJF33Jiw1cTACl7/Ga.p08jy4aGhLwLm'),
    ('resident502@jaswanti.com', '$2a$10$BcYv0QoyFho1jsBMxyWJ5OQYV77B9sL1IixkQj8TJ.ZuWG3hKYLzW'),
    ('resident503@jaswanti.com', '$2a$10$ehiGDBUFlmoMsNzXOYxqz.kfzHKzmx9h0ln9TYmL4DYd.2f5/G32y'),
    ('resident504@jaswanti.com', '$2a$10$BBJel9/uBSstMXwVXI0L5uf9y9BAOHD95pUp.Y6gr9yGlJpVXMQz2'),
    ('resident505@jaswanti.com', '$2a$10$vctTr2OG8luplRpIut7TtODI1yIZirWL93rTc.IBdMAuR4afBt4xa'),
    ('resident506@jaswanti.com', '$2a$10$JDF0DWyG79PogIb.0lQEkupLkpFsICbqPowWSriP7lVoF6ZKVX98a'),
    ('resident507@jaswanti.com', '$2a$10$NrRE0s1pV1xMuhOYLvSrReg3xDgrTjL2vKLgoeILmxNL17dsuH7qG'),
    ('resident508@jaswanti.com', '$2a$10$1/MQGKQHGsME/U7jzvxKo.BVedMxbN6APL0xjR1EgH/54lRoEbRke'),
    ('resident601@jaswanti.com', '$2a$10$gcYJLaZXd5aKVPJ0NAwc6.MS6INUHt5mrTEA6uQhy4dSj4AtDazcO'),
    ('resident602@jaswanti.com', '$2a$10$zaG2epASBBfSiKqZVCoSYufp3buFJNjVjI5d2H7yy3f/WvqabSQB2'),
    ('resident603@jaswanti.com', '$2a$10$MWEQ8eicF/CmZfO/rwSp5eetmwxWweK/bZrrAH6tw6.FHX20uYeXm'),
    ('resident604@jaswanti.com', '$2a$10$OY4D1oPqRlczNsQM3woayOa/5efgzfkFOakYdeynl2XZC2vPlHLnC'),
    ('resident605@jaswanti.com', '$2a$10$VFApAWBi.KY70YnApjtf3ujhdHkOllwdHxNpjq4EjBHoNjfVv96PK'),
    ('resident606@jaswanti.com', '$2a$10$mYE.uXTL8rSuigIo1T.0yuOh7WE7Pr84RnMTX.nbPPFk5cvyRJKyO'),
    ('resident607@jaswanti.com', '$2a$10$EDi/cpG.ZWEXtFWXz8bOceETDHAzSs8Ci2bvLL56nSvYJoBCUZTp2'),
    ('resident608@jaswanti.com', '$2a$10$JYEC8oOzQc4QKE28/DtNjuonyhPfARWS5XugF.3u6sfuflFQj60tm'),
    ('resident701@jaswanti.com', '$2a$10$MzjY41t.Xttnco88CNwivuFKr4N5ofBFF6EPUFbDQZP1CKBcefExO'),
    ('resident702@jaswanti.com', '$2a$10$20kStIAIWJmhcO489AqGH.nrf6k2v3w6QW3izJkaQ0xKJfhDxyhOW'),
    ('resident703@jaswanti.com', '$2a$10$f5fi/PQqM9bPTUUgswdn5ex0w55caAw6VyFLkLbRoENu7KOih8yaK'),
    ('resident704@jaswanti.com', '$2a$10$H5eV3amyKrpqSE8sLPDaYOfZ8Wz3etHT7v6m9ILayLrj7dYq0y55S'),
    ('resident705@jaswanti.com', '$2a$10$FCEHVUPM.iJ0HrdXykqVSOtDnV7VT/GLFaMjPOSKvieSzn9loEa4C'),
    ('resident706@jaswanti.com', '$2a$10$pMciObq48EsK8f16bsS4jucPkGY1ucRgpiJ.9Z1TPTuy8ziCYFwGG'),
    ('resident707@jaswanti.com', '$2a$10$jdqgIfN6bJlejRLOHmRe7eiMwgByGCwi.iIt1ZX02Lt3xJxkEs9aC'),
    ('resident708@jaswanti.com', '$2a$10$kTBwz4lnxnC1taZV6usECuEk/NCQSSmukbjva8//Yi.fT32QUQJdm'),
    ('resident801@jaswanti.com', '$2a$10$TPH7uqj/DEh9//3kuu2sMOnxFvPkuFmx9WLC9ej7HzmjMPqUlm2Ie'),
    ('resident802@jaswanti.com', '$2a$10$pKry.yYi7/nEDo7sWiUv9OLJDCCnb8xsx.W8eituKF8y0H889L/DK'),
    ('resident803@jaswanti.com', '$2a$10$mMdnNbYY0E4uatKCNBvjJOHrH.iYApBGa2Eh7qy1nZDmcylH/LxsC'),
    ('resident804@jaswanti.com', '$2a$10$2Zkzn8Dq69iZ/onWc9Pvku8khy1D5hwc0j2/zdJLiFfjBDEF4VXyS'),
    ('resident805@jaswanti.com', '$2a$10$Hg4WVaigRCPotqlqj5Zz/eEh1VL68m8jjh0witMKJstmLv9FzzqzO'),
    ('resident806@jaswanti.com', '$2a$10$e0foTrAqKhHvU6BzvpigzONVzHfvWdqU4Ixw6zICNfRXj0t1p1sDW'),
    ('resident807@jaswanti.com', '$2a$10$h.HsUy5Ote2nND5CkSx2FODvnWJZlkWelZk0gmsLg5ovyg2c6uPLy'),
    ('resident808@jaswanti.com', '$2a$10$ot8WGtGya94WpXqjAgCRm.YNurptEqZy/FZNjz9bTDcpKBzmIPGri'),
    ('resident901@jaswanti.com', '$2a$10$m/UI63PMH3Dll4oUnkqRxOdDEqnL8EW/LzS17Gkjfa/wwCwebnFj.'),
    ('resident902@jaswanti.com', '$2a$10$D5WH/Mz5VWbjb96ZA0psYeL8G7jKHKqryv./6VzNGr1txvo65FNYS'),
    ('resident903@jaswanti.com', '$2a$10$K5Pvvy46w/R7y/gee4XOjuvcUqPLOIg1cTayn6hTmRpmQeQQK8SiC'),
    ('resident904@jaswanti.com', '$2a$10$LDPhj.ZQFrkgZkTndkJ8l.8Z5AFaoYFRb8TtUe2//IYvfQULc9wN.'),
    ('resident905@jaswanti.com', '$2a$10$6R0H9HFPNcy5znwR8pcwnuoECfJk9nbdoqNfxun8pb2ObBQFU5ba2'),
    ('resident906@jaswanti.com', '$2a$10$yvue6PasCDjY.oHveCEffe1MHU9mpXBAlsuXspbxEZt/rywx4UZKC'),
    ('resident907@jaswanti.com', '$2a$10$F7uKXUbllPNoQWSw4s066.qua6rHQkW88Fbj2ehZhMMfRr/LY5.zq'),
    ('resident908@jaswanti.com', '$2a$10$WKxbaM1KZSWT8/OKHPVYPuUsIM9HhtNPLCHgZy.re/tAwOeLOtXE.'),
    ('resident1001@jaswanti.com', '$2a$10$btyJQQngWr6iVa7lwNu.au8O3MqFAN8K6vrMgS6cqCVwWOYMm419m'),
    ('resident1002@jaswanti.com', '$2a$10$yeMYP83i6u18PrEk74mRv.EPQmjLNufx8Zgf2.X7mqoQA3OWr6gHu'),
    ('resident1003@jaswanti.com', '$2a$10$5MWd9FIg7yw.s01PSqdIKeWXCT30ttGW8Hl9fRR3Oj/gzYBFxNPxi'),
    ('resident1004@jaswanti.com', '$2a$10$i/m6A3srSS7NCS11nt7Kz.cYWokG71sYGz.FmsY8Tlx4vlwrewTjG'),
    ('resident1005@jaswanti.com', '$2a$10$bFuERlT25OtELnH9T/pvHuSQUgbGhe1hKA5OyLcp/QscB95VjY/o.'),
    ('resident1006@jaswanti.com', '$2a$10$nuQzq3W7dpkBn2Cp32.MpunSQVfwkKGLnj1UgDaFeAmx03ExWuFzO'),
    ('resident1007@jaswanti.com', '$2a$10$MlEAbJz9NLf5ALRA6OarrOXyC.okgUsR14xoVxje44beWkGxcvFtm'),
    ('resident1008@jaswanti.com', '$2a$10$m94zi1DT9FdMYsvJWQjIB.W/qFqMVVFg.BfYETz6fPNZHKOB7InHu')
);
