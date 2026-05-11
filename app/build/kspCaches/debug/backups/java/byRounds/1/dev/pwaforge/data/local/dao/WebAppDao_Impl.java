package dev.pwaforge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import dev.pwaforge.data.local.entity.WebAppEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WebAppDao_Impl implements WebAppDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WebAppEntity> __insertionAdapterOfWebAppEntity;

  private final EntityDeletionOrUpdateAdapter<WebAppEntity> __deletionAdapterOfWebAppEntity;

  private final EntityDeletionOrUpdateAdapter<WebAppEntity> __updateAdapterOfWebAppEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public WebAppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWebAppEntity = new EntityInsertionAdapter<WebAppEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `web_apps` (`id`,`name`,`url`,`iconPath`,`themeColor`,`backgroundColor`,`description`,`categoryId`,`isolationId`,`isFullscreen`,`adBlockEnabled`,`translateEnabled`,`translateTarget`,`showTranslateButton`,`autoTranslateOnLoad`,`uaMode`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WebAppEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUrl());
        if (entity.getIconPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIconPath());
        }
        if (entity.getThemeColor() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getThemeColor());
        }
        if (entity.getBackgroundColor() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBackgroundColor());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getDescription());
        }
        if (entity.getCategoryId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getCategoryId());
        }
        statement.bindString(9, entity.getIsolationId());
        final int _tmp = entity.isFullscreen() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.getAdBlockEnabled() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.getTranslateEnabled() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        statement.bindString(13, entity.getTranslateTarget());
        final int _tmp_3 = entity.getShowTranslateButton() ? 1 : 0;
        statement.bindLong(14, _tmp_3);
        final int _tmp_4 = entity.getAutoTranslateOnLoad() ? 1 : 0;
        statement.bindLong(15, _tmp_4);
        statement.bindString(16, entity.getUaMode());
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfWebAppEntity = new EntityDeletionOrUpdateAdapter<WebAppEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `web_apps` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WebAppEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfWebAppEntity = new EntityDeletionOrUpdateAdapter<WebAppEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `web_apps` SET `id` = ?,`name` = ?,`url` = ?,`iconPath` = ?,`themeColor` = ?,`backgroundColor` = ?,`description` = ?,`categoryId` = ?,`isolationId` = ?,`isFullscreen` = ?,`adBlockEnabled` = ?,`translateEnabled` = ?,`translateTarget` = ?,`showTranslateButton` = ?,`autoTranslateOnLoad` = ?,`uaMode` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WebAppEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUrl());
        if (entity.getIconPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIconPath());
        }
        if (entity.getThemeColor() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getThemeColor());
        }
        if (entity.getBackgroundColor() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBackgroundColor());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getDescription());
        }
        if (entity.getCategoryId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getCategoryId());
        }
        statement.bindString(9, entity.getIsolationId());
        final int _tmp = entity.isFullscreen() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.getAdBlockEnabled() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.getTranslateEnabled() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        statement.bindString(13, entity.getTranslateTarget());
        final int _tmp_3 = entity.getShowTranslateButton() ? 1 : 0;
        statement.bindLong(14, _tmp_3);
        final int _tmp_4 = entity.getAutoTranslateOnLoad() ? 1 : 0;
        statement.bindLong(15, _tmp_4);
        statement.bindString(16, entity.getUaMode());
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        statement.bindLong(19, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM web_apps WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WebAppEntity entity, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWebAppEntity.insertAndReturnId(entity);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final WebAppEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWebAppEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final WebAppEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWebAppEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WebAppEntity>> getAll() {
    final String _sql = "SELECT * FROM web_apps ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"web_apps"}, new Callable<List<WebAppEntity>>() {
      @Override
      @NonNull
      public List<WebAppEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfThemeColor = CursorUtil.getColumnIndexOrThrow(_cursor, "themeColor");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfIsolationId = CursorUtil.getColumnIndexOrThrow(_cursor, "isolationId");
          final int _cursorIndexOfIsFullscreen = CursorUtil.getColumnIndexOrThrow(_cursor, "isFullscreen");
          final int _cursorIndexOfAdBlockEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "adBlockEnabled");
          final int _cursorIndexOfTranslateEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "translateEnabled");
          final int _cursorIndexOfTranslateTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "translateTarget");
          final int _cursorIndexOfShowTranslateButton = CursorUtil.getColumnIndexOrThrow(_cursor, "showTranslateButton");
          final int _cursorIndexOfAutoTranslateOnLoad = CursorUtil.getColumnIndexOrThrow(_cursor, "autoTranslateOnLoad");
          final int _cursorIndexOfUaMode = CursorUtil.getColumnIndexOrThrow(_cursor, "uaMode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WebAppEntity> _result = new ArrayList<WebAppEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WebAppEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpThemeColor;
            if (_cursor.isNull(_cursorIndexOfThemeColor)) {
              _tmpThemeColor = null;
            } else {
              _tmpThemeColor = _cursor.getString(_cursorIndexOfThemeColor);
            }
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpIsolationId;
            _tmpIsolationId = _cursor.getString(_cursorIndexOfIsolationId);
            final boolean _tmpIsFullscreen;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFullscreen);
            _tmpIsFullscreen = _tmp != 0;
            final boolean _tmpAdBlockEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAdBlockEnabled);
            _tmpAdBlockEnabled = _tmp_1 != 0;
            final boolean _tmpTranslateEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfTranslateEnabled);
            _tmpTranslateEnabled = _tmp_2 != 0;
            final String _tmpTranslateTarget;
            _tmpTranslateTarget = _cursor.getString(_cursorIndexOfTranslateTarget);
            final boolean _tmpShowTranslateButton;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfShowTranslateButton);
            _tmpShowTranslateButton = _tmp_3 != 0;
            final boolean _tmpAutoTranslateOnLoad;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoTranslateOnLoad);
            _tmpAutoTranslateOnLoad = _tmp_4 != 0;
            final String _tmpUaMode;
            _tmpUaMode = _cursor.getString(_cursorIndexOfUaMode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WebAppEntity(_tmpId,_tmpName,_tmpUrl,_tmpIconPath,_tmpThemeColor,_tmpBackgroundColor,_tmpDescription,_tmpCategoryId,_tmpIsolationId,_tmpIsFullscreen,_tmpAdBlockEnabled,_tmpTranslateEnabled,_tmpTranslateTarget,_tmpShowTranslateButton,_tmpAutoTranslateOnLoad,_tmpUaMode,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WebAppEntity>> getByCategory(final long categoryId) {
    final String _sql = "SELECT * FROM web_apps WHERE categoryId = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"web_apps"}, new Callable<List<WebAppEntity>>() {
      @Override
      @NonNull
      public List<WebAppEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfThemeColor = CursorUtil.getColumnIndexOrThrow(_cursor, "themeColor");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfIsolationId = CursorUtil.getColumnIndexOrThrow(_cursor, "isolationId");
          final int _cursorIndexOfIsFullscreen = CursorUtil.getColumnIndexOrThrow(_cursor, "isFullscreen");
          final int _cursorIndexOfAdBlockEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "adBlockEnabled");
          final int _cursorIndexOfTranslateEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "translateEnabled");
          final int _cursorIndexOfTranslateTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "translateTarget");
          final int _cursorIndexOfShowTranslateButton = CursorUtil.getColumnIndexOrThrow(_cursor, "showTranslateButton");
          final int _cursorIndexOfAutoTranslateOnLoad = CursorUtil.getColumnIndexOrThrow(_cursor, "autoTranslateOnLoad");
          final int _cursorIndexOfUaMode = CursorUtil.getColumnIndexOrThrow(_cursor, "uaMode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WebAppEntity> _result = new ArrayList<WebAppEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WebAppEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpThemeColor;
            if (_cursor.isNull(_cursorIndexOfThemeColor)) {
              _tmpThemeColor = null;
            } else {
              _tmpThemeColor = _cursor.getString(_cursorIndexOfThemeColor);
            }
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpIsolationId;
            _tmpIsolationId = _cursor.getString(_cursorIndexOfIsolationId);
            final boolean _tmpIsFullscreen;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFullscreen);
            _tmpIsFullscreen = _tmp != 0;
            final boolean _tmpAdBlockEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAdBlockEnabled);
            _tmpAdBlockEnabled = _tmp_1 != 0;
            final boolean _tmpTranslateEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfTranslateEnabled);
            _tmpTranslateEnabled = _tmp_2 != 0;
            final String _tmpTranslateTarget;
            _tmpTranslateTarget = _cursor.getString(_cursorIndexOfTranslateTarget);
            final boolean _tmpShowTranslateButton;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfShowTranslateButton);
            _tmpShowTranslateButton = _tmp_3 != 0;
            final boolean _tmpAutoTranslateOnLoad;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoTranslateOnLoad);
            _tmpAutoTranslateOnLoad = _tmp_4 != 0;
            final String _tmpUaMode;
            _tmpUaMode = _cursor.getString(_cursorIndexOfUaMode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WebAppEntity(_tmpId,_tmpName,_tmpUrl,_tmpIconPath,_tmpThemeColor,_tmpBackgroundColor,_tmpDescription,_tmpCategoryId,_tmpIsolationId,_tmpIsFullscreen,_tmpAdBlockEnabled,_tmpTranslateEnabled,_tmpTranslateTarget,_tmpShowTranslateButton,_tmpAutoTranslateOnLoad,_tmpUaMode,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super WebAppEntity> $completion) {
    final String _sql = "SELECT * FROM web_apps WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WebAppEntity>() {
      @Override
      @Nullable
      public WebAppEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfIconPath = CursorUtil.getColumnIndexOrThrow(_cursor, "iconPath");
          final int _cursorIndexOfThemeColor = CursorUtil.getColumnIndexOrThrow(_cursor, "themeColor");
          final int _cursorIndexOfBackgroundColor = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundColor");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfIsolationId = CursorUtil.getColumnIndexOrThrow(_cursor, "isolationId");
          final int _cursorIndexOfIsFullscreen = CursorUtil.getColumnIndexOrThrow(_cursor, "isFullscreen");
          final int _cursorIndexOfAdBlockEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "adBlockEnabled");
          final int _cursorIndexOfTranslateEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "translateEnabled");
          final int _cursorIndexOfTranslateTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "translateTarget");
          final int _cursorIndexOfShowTranslateButton = CursorUtil.getColumnIndexOrThrow(_cursor, "showTranslateButton");
          final int _cursorIndexOfAutoTranslateOnLoad = CursorUtil.getColumnIndexOrThrow(_cursor, "autoTranslateOnLoad");
          final int _cursorIndexOfUaMode = CursorUtil.getColumnIndexOrThrow(_cursor, "uaMode");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final WebAppEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpIconPath;
            if (_cursor.isNull(_cursorIndexOfIconPath)) {
              _tmpIconPath = null;
            } else {
              _tmpIconPath = _cursor.getString(_cursorIndexOfIconPath);
            }
            final String _tmpThemeColor;
            if (_cursor.isNull(_cursorIndexOfThemeColor)) {
              _tmpThemeColor = null;
            } else {
              _tmpThemeColor = _cursor.getString(_cursorIndexOfThemeColor);
            }
            final String _tmpBackgroundColor;
            if (_cursor.isNull(_cursorIndexOfBackgroundColor)) {
              _tmpBackgroundColor = null;
            } else {
              _tmpBackgroundColor = _cursor.getString(_cursorIndexOfBackgroundColor);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Long _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            }
            final String _tmpIsolationId;
            _tmpIsolationId = _cursor.getString(_cursorIndexOfIsolationId);
            final boolean _tmpIsFullscreen;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFullscreen);
            _tmpIsFullscreen = _tmp != 0;
            final boolean _tmpAdBlockEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAdBlockEnabled);
            _tmpAdBlockEnabled = _tmp_1 != 0;
            final boolean _tmpTranslateEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfTranslateEnabled);
            _tmpTranslateEnabled = _tmp_2 != 0;
            final String _tmpTranslateTarget;
            _tmpTranslateTarget = _cursor.getString(_cursorIndexOfTranslateTarget);
            final boolean _tmpShowTranslateButton;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfShowTranslateButton);
            _tmpShowTranslateButton = _tmp_3 != 0;
            final boolean _tmpAutoTranslateOnLoad;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoTranslateOnLoad);
            _tmpAutoTranslateOnLoad = _tmp_4 != 0;
            final String _tmpUaMode;
            _tmpUaMode = _cursor.getString(_cursorIndexOfUaMode);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new WebAppEntity(_tmpId,_tmpName,_tmpUrl,_tmpIconPath,_tmpThemeColor,_tmpBackgroundColor,_tmpDescription,_tmpCategoryId,_tmpIsolationId,_tmpIsFullscreen,_tmpAdBlockEnabled,_tmpTranslateEnabled,_tmpTranslateTarget,_tmpShowTranslateButton,_tmpAutoTranslateOnLoad,_tmpUaMode,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
