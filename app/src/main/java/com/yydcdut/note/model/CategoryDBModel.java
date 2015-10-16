package com.yydcdut.note.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.yydcdut.note.NoteApplication;
import com.yydcdut.note.bean.Category;
import com.yydcdut.note.bean.PhotoNote;
import com.yydcdut.note.model.sqlite.NotesSQLite;
import com.yydcdut.note.utils.compare.ComparatorFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuyidong on 15/7/17.
 */
public class CategoryDBModel extends AbsNotesDBModel implements IModel {

    private List<Category> mCache;

    private static CategoryDBModel sInstance = new CategoryDBModel();

    private CategoryDBModel() {
        findAll();
    }

    public static CategoryDBModel getInstance() {
        return sInstance;
    }

    public List<Category> findAll() {
        if (mCache == null) {
            mCache = new ArrayList<>();
            SQLiteDatabase db = mNotesSQLite.getReadableDatabase();
            Cursor cursor = db.query(NotesSQLite.TABLE_CATEGORY, null, null, null, null, null, "sort asc");
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                String label = cursor.getString(cursor.getColumnIndex("label"));
                int photosNumber = cursor.getInt(cursor.getColumnIndex("photosNumber"));
                boolean isCheck = cursor.getInt(cursor.getColumnIndex("isCheck")) == 0 ? false : true;
                int sort = cursor.getInt(cursor.getColumnIndex("sort"));
                Category category = new Category(id, label, photosNumber, sort, isCheck);
                mCache.add(category);
            }
            cursor.close();
            db.close();
        }
        return mCache;
    }

    public List<Category> refresh() {
        if (mCache == null) {
            mCache = new ArrayList<>();
        } else {
            mCache.clear();
        }
        SQLiteDatabase db = mNotesSQLite.getReadableDatabase();
        Cursor cursor = db.query(NotesSQLite.TABLE_CATEGORY, null, null, null, null, null, "sort asc");
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            String label = cursor.getString(cursor.getColumnIndex("label"));
            int photosNumber = cursor.getInt(cursor.getColumnIndex("photosNumber"));
            boolean isCheck = cursor.getInt(cursor.getColumnIndex("isCheck")) == 0 ? false : true;
            int sort = cursor.getInt(cursor.getColumnIndex("sort"));
            Category category = new Category(id, label, photosNumber, sort, isCheck);
            mCache.add(category);
        }
        cursor.close();
        db.close();
        return mCache;
    }

    /**
     * 针对Category，在DrawerLayout中的菜单的选中
     *
     * @param updateCategory
     * @return
     */
    public boolean setCategoryMenuPosition(Category updateCategory) {
        for (Category category : mCache) {
            if (category.getLabel().equals(updateCategory.getLabel())) {
                category.setCheck(true);
                updateData(updateCategory);
            } else {
                if (category.isCheck()) {
                    category.setCheck(false);
                    updateData(updateCategory);
                }
            }
        }
        return true;
    }

    /**
     * 保持分类，并且刷新
     *
     * @param category
     * @return
     */
    public boolean saveCategory(Category category) {
        if (checkLabelExist(category)) {
            return false;
        }
        long id = saveData(category);
        if (id >= 0) {
            refresh();
        }
        return id >= 0;
    }

    /**
     * 更新分类，并且刷新
     *
     * @param categoryList
     * @return
     */
    public boolean updateCategoryList(List<Category> categoryList) {
        boolean bool = true;
        for (Category category : categoryList) {
            bool &= updateData(category);
        }
        if (bool) {
            refresh();
        }
        return bool;
    }

    public boolean update(Category category) {
        boolean bool = true;
        if (!checkLabelExist(category)) {
            bool &= updateData(category);
        }
        if (bool) {
            refresh();
        }
        return bool;
    }

    /**
     * todo 时间会比较长
     *
     * @param originalLabel
     * @param newLabel
     * @return
     */
    public boolean updateLabel(String originalLabel, String newLabel) {
        boolean bool = true;
        bool &= checkLabelExist(newLabel);
        if (bool) {
            Category category = findByCategoryLabel(originalLabel);
            category.setLabel(newLabel);
            bool &= updateData(category);
            if (bool) {
                //处理PhotoNote
                List<PhotoNote> photoNoteList = PhotoNoteDBModel.getInstance().findByCategoryLabel(originalLabel, ComparatorFactory.FACTORY_NOT_SORT);
                for (PhotoNote photoNote : photoNoteList) {
                    photoNote.setCategoryLabel(newLabel);
                }
                PhotoNoteDBModel.getInstance().updateAll(photoNoteList);
            }
        }
        if (bool) {
            refresh();
        }
        return bool;
    }

    /**
     * 通过label查抄
     *
     * @param categoryLabel
     * @return
     */
    public Category findByCategoryLabel(String categoryLabel) {
        for (Category category : mCache) {
            if (category.getLabel().equals(categoryLabel)) {
                return category;
            }
        }
        return null;
    }

    /**
     * 更新顺序
     *
     * @param categoryList
     * @return
     */
    public boolean updateOrder(List<Category> categoryList) {
        boolean bool = true;
        for (int i = 0; i < categoryList.size(); i++) {
            Category category = categoryList.get(i);
            category.setSort(i);
            bool &= updateData(category);
        }
        refresh();
        return bool;
    }

    public void delete(Category category) {
        final String label = category.getLabel();
        mCache.remove(category);
        deleteData(category);
        NoteApplication.getInstance().getExecutorPool().execute(new Runnable() {
            @Override
            public void run() {
                PhotoNoteDBModel.getInstance().deleteByCategory(label);
            }
        });
    }

    private int deleteData(Category category) {
        SQLiteDatabase db = mNotesSQLite.getWritableDatabase();
        int rows = db.delete(NotesSQLite.TABLE_CATEGORY, "_id = ?", new String[]{category.getId() + ""});
        db.close();
        return rows;
    }

    private boolean checkLabelExist(Category category) {
        for (Category item : mCache) {
            if (item.getLabel().equals(category.getLabel())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLabelExist(String newLabel) {
        for (Category item : mCache) {
            if (item.getLabel().equals(newLabel)) {
                return true;
            }
        }
        return false;
    }

    private long saveData(Category category) {
        SQLiteDatabase db = mNotesSQLite.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("label", category.getLabel());
        contentValues.put("photosNumber", category.getPhotosNumber());
        contentValues.put("isCheck", category.isCheck());
        contentValues.put("sort", category.getSort());
        long id = db.insert(NotesSQLite.TABLE_CATEGORY, null, contentValues);
        db.close();
        return id;
    }

    private boolean updateData(Category category) {
        SQLiteDatabase db = mNotesSQLite.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("label", category.getLabel());
        contentValues.put("photosNumber", category.getPhotosNumber());
        contentValues.put("isCheck", category.isCheck());
        contentValues.put("sort", category.getSort());
        int rows = db.update(NotesSQLite.TABLE_CATEGORY, contentValues, "_id = ?", new String[]{category.getId() + ""});
        db.close();
        return rows >= 0;
    }
}