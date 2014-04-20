package kx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

import kx.K.Flip;

import org.apache.commons.lang.StringUtils;

public class Dumper
{
    private static int ROW_LIMIT = 25;
    private static int COL_LIMIT = 80;
    
    private static int rowLimit = ROW_LIMIT;
    private static int colLimit = COL_LIMIT;
    private static String DOT_DOT_DOT = new String("...");
    
    public static void setLimits(int row, int col)
    {
        rowLimit = row;
        colLimit = col;
    }
    
    public static String toString(Object obj)
    {
        StringWriter sw = new StringWriter();
        if (obj == null)
            return "null";
        try
        {
            dump(obj, sw);
        }
        catch (Exception e)
        {
            e.printStackTrace(new PrintWriter(sw));
        }
        return sw.toString();
    }
    
    public static void dump(Object obj, Writer wr) throws Exception
    {
        if (obj instanceof K.Dict)
            dump((K.Dict) obj, wr, rowLimit, colLimit);
        else if (obj instanceof K.Flip)
        {
            dump( ((K.Flip) obj).toTable(), wr, rowLimit, colLimit );
        }
        else if (obj instanceof K.Table)
        {
            dump( (K.Table) obj, wr, rowLimit, colLimit);
        }
        else if (obj.getClass().isArray())
        {
            Object[] arr = K.autoboxArray(obj);
            if(colLimit < arr.length)
            {
                arr = Arrays.copyOf(arr, colLimit + 1);
                arr[arr.length-1] = DOT_DOT_DOT;  // usurp the last one...
            }
            wr.write(Arrays.deepToString(arr));
        }
        else
        {
            wr.write(obj.toString());
        }
    }

    private static void dump(K.Dict d, Writer wr, int rowLimit, int colLimit) throws Exception
    {
        if (d.x instanceof K.Flip && d.y instanceof K.Flip)
        {
            // Keyed table
            K.Flip fx = (K.Flip) d.x;
            K.Flip fy = (K.Flip) d.y;

            int colWidth[] = new int[colLimit];

            // Determine Column widths
            for (int idx = 0; idx < fx.x.length + fy.x.length + 1 && idx < colLimit; idx++)
            {
                if (idx < fx.x.length)
                {
                    colWidth[idx] = calcColWidth(fx, idx, rowLimit);
                }
                else if (idx == fx.x.length)
                {
                    colWidth[idx] = 1;
                }
                else
                {
                    colWidth[idx] = calcColWidth(fy, idx - (fx.x.length + 1), rowLimit);
                }
            }

            // dump col headers
            for (int idx = 0; idx < fx.x.length + fy.x.length + 1 && idx < colLimit; idx++)
            {
                if (idx < fx.x.length)
                {
                    widthOut(fx.x[idx], colWidth[idx], wr);
                }
                else if (idx == fx.x.length)
                {
                    wr.write("|");
                }
                else
                {
                    widthOut(fy.x[idx - (fx.x.length + 1)], colWidth[idx], wr);
                }
            }
            end (wr, (fx.x.length + fy.x.length + 1) > colLimit);
            
            // header delim
            separator(wr, colWidth, colLimit);

            // dump rows
            int rowCount = findRowcount(fx, fy);
            for (int ri = 0; ri < rowCount && ri < rowLimit; ri++)
            {
                for (int idx = 0; idx < fx.x.length + fy.x.length + 1 && idx < colLimit; idx++)
                {
                    if (idx < fx.x.length)
                    {
                        widthOutObj(fx.y[idx], ri, colWidth[idx], wr);
                    }
                    else if (idx == fx.x.length)
                    {
                        wr.write("|");
                    }
                    else
                    {
                        widthOutObj(fy.y[idx - (fx.x.length + 1)], ri, colWidth[idx], wr);
                    }
                }
                end (wr, (fx.x.length + fy.x.length + 1) > colLimit);
            }
            end (wr, rowCount > rowLimit);
        }
        else
        {
            Map<Object, Object> m = d.toMap();
            int kw = 1;
            for (Map.Entry<Object, Object> e : m.entrySet())
            {
                kw =  Math.max(kw, e.getKey().toString().length());
            }
            for (Map.Entry<Object, Object> e : m.entrySet())
            {
                if (e.getValue() instanceof K.Flip || e.getValue() instanceof K.Dict)
                {
                    wr.write(e.getKey() + ":\n");
                    dump(e.getValue(), wr);
                }
                else
                {
                    String fmt = "%1$-" + kw + "s | %2$s\n";
                    // wr.write(String.format(fmt, e.getKey(), ToStringBuilder.reflectionToString(e.getValue())));
                    wr.write(String.format(fmt, e.getKey(), toString(e.getValue())));
                }
            }
        }
    }

    private static void dump(K.Table table, Writer wr, int rowLimit, int colLimit) throws Exception
    {
        int colWidth[] = new int[table.columns.size()];
        
        int col = 0;
        for (Map.Entry<String, Object[]> e : table.columns.entrySet())
        {
            colWidth[col] = e.getKey().length();
            for (Object o : e.getValue())
            {
                String s = toString(o);
                colWidth[col] = Math.max(colWidth[col], s.length());
            }
            colWidth[col]++; // account for space between
            col++;
        }
        
        // dump col headers
        int rowCount = 0;
        Object[] hdrs = table.columns.keySet().toArray();
        for (int i = 0; i < hdrs.length && i < colLimit; i++)
        {
            widthOut((String) hdrs[i], colWidth[i], wr);
            Object[] column = table.columns.get(hdrs[i]);
            rowCount = column.length;
        }
        
        end(wr, hdrs.length > colLimit);
        
        // header delim
        separator(wr, colWidth, colLimit);
        
        // dump rows
        for(int r = 0; r < rowCount && r < rowLimit; r++)
        {
            for(int c = 0; c < hdrs.length && c < colLimit; c++)
            {
                Object[] column = table.columns.get(hdrs[c]);
                widthOut(toString(column[r]), colWidth[c], wr);
            }
            end(wr, false);
        }

        end (wr, rowCount > rowLimit);
    }

    private static String dump(K.Flip f, Writer wr, int rowLimit, int colLimit) throws Exception
    {
        int colWidth[] = new int[colLimit];

        // Determine Column widths
        for (int idx = 0; idx < f.x.length && idx < colLimit; idx++)
        {
            colWidth[idx] = calcColWidth(f, idx, rowLimit);
        }

        // dump col headers
        for (int idx = 0; idx < f.x.length && idx < colLimit; idx++)
        {
            if (idx < f.x.length)
            {
                widthOut(f.x[idx], colWidth[idx], wr);
            }
        }
        end (wr, (f.x.length) > colLimit);

        // header delim
        separator(wr, colWidth, colLimit);

        // dump rows
        int rowCount = findRowcount(f);
        for (int ri = 0; ri < rowCount && ri < rowLimit; ri++)
        {
            for (int idx = 0; idx < f.x.length && idx < colLimit; idx++)
            {
                widthOutObj(f.y[idx], ri, colWidth[idx], wr);
            }
            end(wr, f.x.length > colLimit);
        }
        end (wr, rowCount > rowLimit);

        return wr.toString();
    }

    private static void separator(Writer wr, int[] colWidth, int colLimit) throws IOException
    {
        // header delim
        int wid = 0;
        for (int idx = 0; idx < colWidth.length && idx < colLimit; idx++)
        {
            wid += colWidth[idx];
        }
        // wid -= colWidth.length; // overshoots... 
        wr.write( StringUtils.repeat("-", wid) + "\n" );
    }

    private static void end(Writer wr, boolean b) throws IOException
    {
        if (b)
        {
            wr.write("...");
        }
        wr.write("\n");
    }

    /**
     * @param object
     * @param ri
     * @param i
     * @throws Exception 
     */
    private static void widthOutObj(Object col, int ri, int i, Writer wr) throws Exception
    {
        widthOut(colRow2Text(col, ri), i, wr);
    }

    /**
     * @param fx
     * @param fy
     * @return
     */
    private static int findRowcount(Flip fx, Flip fy)
    {
        return Math.min(findRowcount(fx), findRowcount(fy));
    }

    /**
     * @param fx
     * @return
     */
    private static int findRowcount(Flip f)
    {
        int rowCount = 1;

        for (int idx = 0; idx < f.y.length; idx++)
        {
            rowCount = Math.max(rowCount, oLen(f.y[idx]));
        }

        return rowCount;
    }

    public static int oLen(Object arr)
    {
        return arr.getClass().isArray() ? Array.getLength(arr) : -1;
    }

    /**
     * @param string
     * @param wid
     * @throws Exception 
     */
    private static void widthOut(String msg, int wid, Writer wr) throws Exception
    {
        String fmt = "%1$-" + wid + "s";
        wr.write(String.format(fmt, msg));
    }

    private static int getRowCount(K.Flip f, int idx)
    {
        int rowCount = 1;

        if (f.y[idx].getClass().isArray())
        {
            rowCount = Math.max(rowCount, Array.getLength(f.y[idx]));
        }

        return rowCount;
    }

    private static int calcColWidth(K.Flip f, int colIdx, int rowLimit)
    {
        int colWidth = f.x[colIdx].length();

        int rowCount = getRowCount(f, colIdx);
        for (int idx = 0; idx < rowCount && idx < rowLimit; idx++)
        {
            int recLen = colRow2Text(f.y[colIdx], idx).length();
            colWidth = Math.max(recLen, colWidth);
        }

        return colWidth + 1;
    }

    private static String colRow2Text(Object col, int ri)
    {
        Object[] arr = K.autoboxArray(col);
        try
        {
            return arr[ri].toString();
        }
        catch (ArrayIndexOutOfBoundsException ae)
        {
            return "";
        }
    }
}
