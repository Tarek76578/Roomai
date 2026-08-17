def commit_generation():
    key = usage_key()
    month = current_month()

    connection = db()

    try:
        # Lock DB to avoid races while checking reserved state.
        connection.execute("BEGIN IMMEDIATE")

        row = connection.execute(
            """
            SELECT used, reserved
            FROM usage
            WHERE device_id = ?
              AND month = ?
            """,
            (key, month)
        ).fetchone()

        if not row:
            # No row: nothing reserved to commit. Make sure a row exists for future bookkeeping.
            connection.execute(
                """
                INSERT OR IGNORE INTO usage (
                    device_id, month, used, reserved
                ) VALUES (?, ?, 0, 0)
                """,
                (key, month)
            )
            connection.commit()
            return

        used, reserved = row

        # Only commit when a reservation exists.
        if reserved > 0:
            connection.execute(
                """
                UPDATE usage
                SET reserved = reserved - 1,
                    used = used + 1
                WHERE device_id = ?
                  AND month = ?
                """,
                (key, month)
            )
            connection.commit()
        else:
            # No reservation to commit: rollback and do not increment used.
            connection.rollback()
            return

    except Exception:
        connection.rollback()
        raise

    finally:
        connection.close()
