export const activeOrderSql = `SELECT id, status FROM orders WHERE status = 'NEW'`;
