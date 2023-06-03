UPDATE net_sf_jmoney_entry SET "type"='acquisition-or-disposal' WHERE "_ID"=104666160 AND "type" IS NULL

SELECT "type" from net_sf_jmoney_entry group by "type"

SELECT "type" from net_sf_jmoney_transaction group by "type"

SELECT COUNT(*) from net_sf_jmoney_entry entry JOIN net_sf_jmoney_transaction trans ON entry."net_sf_jmoney_transaction_entry" = trans."_ID" where trans."type" IS NOT NULL AND entry."type" IS NOT NULL

SELECT account."name", entry.* from net_sf_jmoney_entry entry JOIN net_sf_jmoney_account account ON entry."account" = account."_ID" ORDER BY "creation" DESC FETCH FIRST 300 ROWS ONLY

SELECT "date", entry."_ID", "creation" from net_sf_jmoney_entry entry JOIN net_sf_jmoney_transaction trans ON entry."net_sf_jmoney_transaction_entry" = trans."_ID" where entry."_ID" < 102000 ORDER BY "creation" DESC FETCH FIRST 1000 ROWS ONLY
SELECT * from net_sf_jmoney_entry entry ORDER BY "creation" DESC FETCH FIRST 300 ROWS ONLY
