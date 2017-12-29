/*
 * Copyright (C) 2017 team-cachebox.de
 *
 * Licensed under the : GNU General  License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.gdx.sqlite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by Longri on 18.12.2017.
 */
class GdxSqliteTest {

    static {
        TestUtils.initialGdx();
    }

    public GdxSqliteTest() {
    } //constructor for core test reflection

    static FileHandle testFolder = Gdx.files.local("GdxSqlite/testResources");

    @BeforeAll
    static void setUp() {
        //load natives
        TestUtils.loadSharedLib("GdxSqlite");
        testFolder.mkdirs();
    }

    @AfterAll
    static void tearDown() {
        assertThat("Test folder must be deleted after cleanup", testFolder.deleteDirectory());
    }

    @Test
    void getSqliteVersion() {
        assertEquals("3.21.0", GdxSqlite.getSqliteVersion(), "SQLite version must be correct");
    }

    @Test
    void openOrCreateDatabase() {

        FileHandle dbFileHandle = testFolder.child("createTest.db3");
        GdxSqlite db = new GdxSqlite(dbFileHandle);
        db.openOrCreateDatabase();

        final String CREATE = "CREATE TABLE Test (\n" +
                "    Id          INTEGER        NOT NULL\n" +
                "                               PRIMARY KEY AUTOINCREMENT,\n" +
                "    testName NVARCHAR (255)\n" +
                ");";

        db.execSQL(CREATE);
        db.closeDatabase();

        String dbFileString = dbFileHandle.readString();
        assertThat("DB file must created", dbFileString.startsWith("SQLite format 3"));
    }

    @Test
    void execSQL() {
    }

    @Test
    void rawQuery() {
    }

    @Test
    void rawQuery1() {
    }
}