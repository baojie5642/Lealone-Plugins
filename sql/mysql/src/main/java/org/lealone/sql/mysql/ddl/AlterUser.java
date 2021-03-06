/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.sql.mysql.ddl;

import org.lealone.api.ErrorCode;
import org.lealone.common.exceptions.DbException;
import org.lealone.db.Database;
import org.lealone.db.LealoneDatabase;
import org.lealone.db.ServerSession;
import org.lealone.db.auth.Auth;
import org.lealone.db.auth.User;
import org.lealone.sql.SQLStatement;
import org.lealone.sql.mysql.expression.Expression;

/**
 * This class represents the statements
 * ALTER USER ADMIN,
 * ALTER USER RENAME,
 * ALTER USER SET PASSWORD
 */
public class AlterUser extends DefineStatement {

    private int type;
    private User user;
    private String newName;
    private Expression password;
    private Expression salt;
    private Expression hash;
    private boolean admin;

    public AlterUser(ServerSession session) {
        super(session);
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setSalt(Expression e) {
        salt = e;
    }

    public void setHash(Expression e) {
        hash = e;
    }

    public void setPassword(Expression password) {
        this.password = password;
    }

    @Override
    public int update() {
        session.commit(true);
        Database db = LealoneDatabase.getInstance();
        switch (type) {
        case SQLStatement.ALTER_USER_SET_PASSWORD:
            if (user != session.getUser()) {
                session.getUser().checkAdmin();
            }
            if (hash != null && salt != null) {
                CreateUser.setSaltAndHash(user, session, salt, hash);
            } else {
                CreateUser.setPassword(user, session, password);
            }
            break;
        case SQLStatement.ALTER_USER_RENAME:
            session.getUser().checkAdmin();
            if (Auth.findUser(newName) != null || newName.equals(user.getName())) {
                throw DbException.get(ErrorCode.USER_ALREADY_EXISTS_1, newName);
            }
            db.renameDatabaseObject(session, user, newName);
            break;
        case SQLStatement.ALTER_USER_ADMIN:
            session.getUser().checkAdmin();
            if (!admin) {
                user.checkOwnsNoSchemas(session);
            }
            user.setAdmin(admin);
            break;
        default:
            DbException.throwInternalError("type=" + type);
        }
        db.updateMeta(session, user);
        return 0;
    }

    @Override
    public int getType() {
        return type;
    }

}
