package dev.pwaforge.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import dev.pwaforge.data.local.dao.CategoryDao;
import dev.pwaforge.data.local.dao.CategoryDao_Impl;
import dev.pwaforge.data.local.dao.WebAppDao;
import dev.pwaforge.data.local.dao.WebAppDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WebAppDao _webAppDao;

  private volatile CategoryDao _categoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `web_apps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `iconPath` TEXT, `themeColor` TEXT, `backgroundColor` TEXT, `description` TEXT, `categoryId` INTEGER, `isolationId` TEXT NOT NULL, `isFullscreen` INTEGER NOT NULL, `adBlockEnabled` INTEGER NOT NULL, `translateEnabled` INTEGER NOT NULL, `translateTarget` TEXT NOT NULL, `showTranslateButton` INTEGER NOT NULL, `autoTranslateOnLoad` INTEGER NOT NULL, `uaMode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_web_apps_categoryId` ON `web_apps` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_web_apps_updatedAt` ON `web_apps` (`updatedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sortIndex` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '67e120903c8b2e8a085946ef91d69d6b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `web_apps`");
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWebApps = new HashMap<String, TableInfo.Column>(18);
        _columnsWebApps.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("iconPath", new TableInfo.Column("iconPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("themeColor", new TableInfo.Column("themeColor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("backgroundColor", new TableInfo.Column("backgroundColor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("categoryId", new TableInfo.Column("categoryId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("isolationId", new TableInfo.Column("isolationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("isFullscreen", new TableInfo.Column("isFullscreen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("adBlockEnabled", new TableInfo.Column("adBlockEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("translateEnabled", new TableInfo.Column("translateEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("translateTarget", new TableInfo.Column("translateTarget", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("showTranslateButton", new TableInfo.Column("showTranslateButton", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("autoTranslateOnLoad", new TableInfo.Column("autoTranslateOnLoad", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("uaMode", new TableInfo.Column("uaMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWebApps.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWebApps = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysWebApps.add(new TableInfo.ForeignKey("categories", "SET NULL", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWebApps = new HashSet<TableInfo.Index>(2);
        _indicesWebApps.add(new TableInfo.Index("index_web_apps_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesWebApps.add(new TableInfo.Index("index_web_apps_updatedAt", false, Arrays.asList("updatedAt"), Arrays.asList("ASC")));
        final TableInfo _infoWebApps = new TableInfo("web_apps", _columnsWebApps, _foreignKeysWebApps, _indicesWebApps);
        final TableInfo _existingWebApps = TableInfo.read(db, "web_apps");
        if (!_infoWebApps.equals(_existingWebApps)) {
          return new RoomOpenHelper.ValidationResult(false, "web_apps(dev.pwaforge.data.local.entity.WebAppEntity).\n"
                  + " Expected:\n" + _infoWebApps + "\n"
                  + " Found:\n" + _existingWebApps);
        }
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(3);
        _columnsCategories.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("sortIndex", new TableInfo.Column("sortIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(dev.pwaforge.data.local.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "67e120903c8b2e8a085946ef91d69d6b", "78dcc66d10d0fb072950fcf73321fb61");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "web_apps","categories");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `web_apps`");
      _db.execSQL("DELETE FROM `categories`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WebAppDao.class, WebAppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WebAppDao webAppDao() {
    if (_webAppDao != null) {
      return _webAppDao;
    } else {
      synchronized(this) {
        if(_webAppDao == null) {
          _webAppDao = new WebAppDao_Impl(this);
        }
        return _webAppDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }
}
