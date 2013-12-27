package ru.spbau.mit.dbmsau;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import ru.spbau.mit.dbmsau.command.AbstractSQLCommand;
import ru.spbau.mit.dbmsau.index.FileIndexManager;
import ru.spbau.mit.dbmsau.pages.PageManager;
import ru.spbau.mit.dbmsau.pages.StubPageManager;
import ru.spbau.mit.dbmsau.pages.exception.PageManagerInitException;
import ru.spbau.mit.dbmsau.relation.Column;
import ru.spbau.mit.dbmsau.relation.Type;
import ru.spbau.mit.dbmsau.syntax.SyntaxAnalyzer;
import ru.spbau.mit.dbmsau.table.FileTableManager;
import ru.spbau.mit.dbmsau.table.Table;
import ru.spbau.mit.dbmsau.table.TableManager;
import ru.spbau.mit.dbmsau.table.TableRecordManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class BaseTest extends Assert {
    protected static final int TEST_COLUMN_INDEX_ID = 0;
    protected static final int TEST_COLUMN_INDEX_NAME = 1;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected Context context;

    protected Context buildContext() {
        Context context = new Context(tempFolder.getRoot().getPath());
        context.setPageManager(buildPageManager(context));
        context.setTableManager(buildTableManager(context));
        context.setTableRecordManager(buildTableRecordManager(context));
        context.setIndexManager(new FileIndexManager(context));

        try {
            context.init();
        } catch (PageManagerInitException e) {
            assertTrue(false);
        }

        return context;
    }

    protected PageManager buildPageManager(Context context) {
        return new StubPageManager(context);
    }

    protected TableManager buildTableManager(Context context) {
        return new FileTableManager(context);
    }

    protected TableRecordManager buildTableRecordManager(Context context) {
        return new TableRecordManager(context);
    }


    @Before
    public void setUp() throws Exception {
        setUpContext();
    }

    protected void checkBusyPages() {
        assertFalse(context.getPageManager().isThereBusyPages());
    }

    protected void setUpContext() throws Exception {
        if (context != null) {
            context.onQuit();
        }

        context = buildContext();
    }

    protected File getResourceFileByName(String resourceName) {
        return FileUtils.toFile(getClass().getResource(resourceName));
    }

    protected void initSQLDumpLoad(String resourceName) throws Exception {
        if (resourceName != null) {
            SyntaxAnalyzer analyzer = new SyntaxAnalyzer(new FileInputStream(
                    getResourceFileByName(resourceName)
            ));

            for (AbstractSQLCommand command : analyzer) {
                command.setContext(context);
                command.execute();
            }
        }
    }

    protected Table buildTestTable() {
        return new Table(
            "test",
            new ArrayList<>(
                Arrays.asList(
                    new Column("test", "id", Type.getIntegerType()),
                    new Column("test", "name", Type.getType("varchar", 50))
                )
            )
        );
    }
}
