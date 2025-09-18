package org.spigotmc.builder;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class JavaVersion
{

    private static JavaVersion latestVersion;
    private static final Map<Integer, JavaVersion> byVersion = new HashMap<>();
    //
    public static final JavaVersion JAVA_5 = new JavaVersion( "Java 5", 49 );
    public static final JavaVersion JAVA_6 = new JavaVersion( "Java 6", 50 );
    public static final JavaVersion JAVA_7 = new JavaVersion( "Java 7", 51 );
    public static final JavaVersion JAVA_8 = new JavaVersion( "Java 8", 52 );
    public static final JavaVersion JAVA_9 = new JavaVersion( "Java 9", 53 );
    public static final JavaVersion JAVA_10 = new JavaVersion( "Java 10", 54 );
    public static final JavaVersion JAVA_11 = new JavaVersion( "Java 11", 55 );
    public static final JavaVersion JAVA_12 = new JavaVersion( "Java 12", 56 );
    public static final JavaVersion JAVA_13 = new JavaVersion( "Java 13", 57 );
    public static final JavaVersion JAVA_14 = new JavaVersion( "Java 14", 58 );
    public static final JavaVersion JAVA_15 = new JavaVersion( "Java 15", 59 );
    public static final JavaVersion JAVA_16 = new JavaVersion( "Java 16", 60 );
    public static final JavaVersion JAVA_17 = new JavaVersion( "Java 17", 61 );
    public static final JavaVersion JAVA_18 = new JavaVersion( "Java 18", 62 );
    public static final JavaVersion JAVA_19 = new JavaVersion( "Java 19", 63 );
    public static final JavaVersion JAVA_20 = new JavaVersion( "Java 20", 64 );
    public static final JavaVersion JAVA_21 = new JavaVersion( "Java 21", 65 );
    public static final JavaVersion JAVA_22 = new JavaVersion( "Java 22", 66 );
    public static final JavaVersion JAVA_23 = new JavaVersion( "Java 23", 67 );
    public static final JavaVersion JAVA_24 = new JavaVersion( "Java 24", 68 );
    public static final JavaVersion JAVA_25 = new JavaVersion( "Java 25", 69 );
    //
    private final String name;
    private final int version;
    private final boolean unknown;

    private JavaVersion(String name, int version)
    {
        this( name, version, false );

        if ( latestVersion == null || version > latestVersion.getVersion() )
        {
            latestVersion = this;
        }
    }

    private JavaVersion(String name, int version, boolean unknown)
    {
        this.name = name;
        this.version = version;
        this.unknown = unknown;

        byVersion.put( version, this );
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static JavaVersion getByVersion(int version)
    {
        JavaVersion java = byVersion.get( version );
        if ( java == null )
        {
            java = new JavaVersion( "Java " + ( version - 44 ) + " [" + version + "]*", version, true );
        }

        return java;
    }

    public static JavaVersion getLatestVersion()
    {
        return latestVersion;
    }

    public static JavaVersion getCurrentVersion()
    {
        return getByVersion( (int) Float.parseFloat( System.getProperty( "java.class.version" ) ) );
    }

    public static String printVersions(int[] versions)
    {
        StringBuilder sb = new StringBuilder();

        sb.append( '[' );
        for ( int v : versions )
        {
            JavaVersion found = getByVersion( v );
            sb.append( found );
            sb.append( ", " );
        }
        sb.setLength( sb.length() - 2 );
        sb.append( ']' );

        return sb.toString();
    }
}
