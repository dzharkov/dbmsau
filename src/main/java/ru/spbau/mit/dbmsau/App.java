package ru.spbau.mit.dbmsau;

import ru.spbau.mit.dbmsau.syntax.SyntaxAnalyzer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        SyntaxAnalyzer a = new SyntaxAnalyzer();

        try {
            a.execute(System.in);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}