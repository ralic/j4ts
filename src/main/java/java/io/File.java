package java.io;

import java.util.ArrayList;
import java.util.List;

/**
 * JSweet implementation for file.
 */

public class File implements Serializable, Comparable<File> {

	/**
	 * The FileSystem object representing the platform's local file system.
	 */
	public static final LocalStorageFileSystem fs = new LocalStorageFileSystem();

	private final String path;

	private static enum PathStatus {
		INVALID, CHECKED
	};

	private transient PathStatus status = null;

	final boolean isInvalid() {
		if (status == null) {
			status = (this.path.indexOf('\u0000') < 0) ? PathStatus.CHECKED : PathStatus.INVALID;
		}
		return status == PathStatus.INVALID;
	}

	private final transient int prefixLength;

	int getPrefixLength() {
		return prefixLength;
	}

	public static final char separatorChar = fs.getSeparator();

	public static final String separator = "" + separatorChar;

	public static final char pathSeparatorChar = fs.getPathSeparator();

	public static final String pathSeparator = "" + pathSeparatorChar;

	private File(String pathname, int prefixLength) {
		this.path = pathname;
		this.prefixLength = prefixLength;
	}

	private File(File parent, String child, boolean direct) {
		assert parent.path != null;
		assert (!parent.path.equals(""));
		this.path = fs.resolve(parent.path, child);
		this.prefixLength = parent.prefixLength;
	}

	public File(String pathname) {
		if (pathname == null) {
			throw new NullPointerException();
		}
		this.path = fs.normalize(pathname);
		this.prefixLength = fs.prefixLength(this.path);
	}

	public File(String parent, String child) {
		if (child == null) {
			throw new NullPointerException();
		}
		if (parent != null) {
			if (parent.equals("")) {
				this.path = fs.resolve(fs.getDefaultParent(), fs.normalize(child));
			} else {
				this.path = fs.resolve(fs.normalize(parent), fs.normalize(child));
			}
		} else {
			this.path = fs.normalize(child);
		}
		this.prefixLength = fs.prefixLength(this.path);
	}

	public File(File parent, String child) {
		if (child == null) {
			throw new NullPointerException();
		}
		if (parent != null) {
			if (parent.path.equals("")) {
				this.path = fs.resolve(fs.getDefaultParent(), fs.normalize(child));
			} else {
				this.path = fs.resolve(parent.path, fs.normalize(child));
			}
		} else {
			this.path = fs.normalize(child);
		}
		this.prefixLength = fs.prefixLength(this.path);
	}

	public String getName() {
		int index = path.lastIndexOf(separatorChar);
		if (index < prefixLength)
			return path.substring(prefixLength);
		return path.substring(index + 1);
	}

	public String getParent() {
		int index = path.lastIndexOf(separatorChar);
		if (index < prefixLength) {
			if ((prefixLength > 0) && (path.length() > prefixLength))
				return path.substring(0, prefixLength);
			return null;
		}
		return path.substring(0, index);
	}

	public File getParentFile() {
		String p = this.getParent();
		if (p == null)
			return null;
		return new File(p, this.prefixLength);
	}

	public String getPath() {
		return path;
	}

	public boolean isAbsolute() {
		return fs.isAbsolute(this);
	}

	public String getAbsolutePath() {
		return fs.resolve(this);
	}

	public File getAbsoluteFile() {
		String absPath = getAbsolutePath();
		return new File(absPath, fs.prefixLength(absPath));
	}

	public String getCanonicalPath() throws IOException {
		if (isInvalid()) {
			throw new IOException("Invalid file path");
		}
		return fs.canonicalize(fs.resolve(this));
	}

	public File getCanonicalFile() throws IOException {
		String canonPath = getCanonicalPath();
		return new File(canonPath, fs.prefixLength(canonPath));
	}

	private static String slashify(String path, boolean isDirectory) {
		String p = path;
		if (File.separatorChar != '/')
			p = p.replace(File.separatorChar, '/');
		if (!p.startsWith("/"))
			p = "/" + p;
		if (!p.endsWith("/") && isDirectory)
			p = p + "/";
		return p;
	}

	// @Deprecated
	// public URL toURL() throws MalformedURLException {
	// if (isInvalid()) {
	// throw new MalformedURLException("Invalid file path");
	// }
	// return new URL("file", "", slashify(getAbsolutePath(), isDirectory()));
	// }

	// public URI toURI() {
	// try {
	// File f = getAbsoluteFile();
	// String sp = slashify(f.getPath(), f.isDirectory());
	// if (sp.startsWith("//"))
	// sp = "//" + sp;
	// return new URI("file", null, sp, null);
	// } catch (URISyntaxException x) {
	// throw new Error(x); // Can't happen
	// }
	// }

	/* -- Attribute accessors -- */

	public boolean canRead() {
		if (isInvalid()) {
			return false;
		}
		return fs.checkAccess(this, FileSystem.ACCESS_READ);
	}

	public boolean canWrite() {
		if (isInvalid()) {
			return false;
		}
		return fs.checkAccess(this, FileSystem.ACCESS_WRITE);
	}

	public boolean exists() {
		if (isInvalid()) {
			return false;
		}
		return ((fs.getBooleanAttributes(this) & FileSystem.BA_EXISTS) != 0);
	}

	public boolean isDirectory() {
		if (isInvalid()) {
			return false;
		}
		return ((fs.getBooleanAttributes(this) & FileSystem.BA_DIRECTORY) != 0);
	}

	public boolean isFile() {
		if (isInvalid()) {
			return false;
		}
		return ((fs.getBooleanAttributes(this) & FileSystem.BA_REGULAR) != 0);
	}

	public boolean isHidden() {
		if (isInvalid()) {
			return false;
		}
		return ((fs.getBooleanAttributes(this) & FileSystem.BA_HIDDEN) != 0);
	}

	public long lastModified() {
		if (isInvalid()) {
			return 0L;
		}
		return fs.getLastModifiedTime(this);
	}

	public long length() {
		if (isInvalid()) {
			return 0L;
		}
		return fs.getLength(this);
	}

	public boolean createNewFile() throws IOException {
		if (isInvalid()) {
			throw new IOException("Invalid file path");
		}
		return fs.createFileExclusively(path);
	}

	public boolean delete() {
		if (isInvalid()) {
			return false;
		}
		return fs.delete(this);
	}

	public String[] list() {
		if (isInvalid()) {
			return null;
		}
		return fs.list(this);
	}

	public String[] list(FilenameFilter filter) {
		String names[] = list();
		if ((names == null) || (filter == null)) {
			return names;
		}
		List<String> v = new ArrayList<>();
		for (int i = 0; i < names.length; i++) {
			if (filter.accept(this, names[i])) {
				v.add(names[i]);
			}
		}
		return v.toArray(new String[v.size()]);
	}

	public File[] listFiles() {
		String[] ss = list();
		if (ss == null)
			return null;
		int n = ss.length;
		File[] fs = new File[n];
		for (int i = 0; i < n; i++) {
			fs[i] = new File(this, ss[i], true);
		}
		return fs;
	}

	public File[] listFiles(FilenameFilter filter) {
		String ss[] = list();
		if (ss == null)
			return null;
		ArrayList<File> files = new ArrayList<>();
		for (String s : ss)
			if ((filter == null) || filter.accept(this, s))
				files.add(new File(this, s, true));
		return files.toArray(new File[files.size()]);
	}

	public File[] listFiles(FileFilter filter) {
		String ss[] = list();
		if (ss == null)
			return null;
		ArrayList<File> files = new ArrayList<>();
		for (String s : ss) {
			File f = new File(this, s, true);
			if ((filter == null) || filter.accept(f))
				files.add(f);
		}
		return files.toArray(new File[files.size()]);
	}

	public boolean mkdir() {
		if (isInvalid()) {
			return false;
		}
		return fs.createDirectory(this);
	}

	public boolean mkdirs() {
		if (exists()) {
			return false;
		}
		if (mkdir()) {
			return true;
		}
		File canonFile = null;
		try {
			canonFile = getCanonicalFile();
		} catch (IOException e) {
			return false;
		}

		File parent = canonFile.getParentFile();
		return (parent != null && (parent.mkdirs() || parent.exists()) && canonFile.mkdir());
	}

	public boolean renameTo(File dest) {
		if (dest == null) {
			throw new NullPointerException();
		}
		if (this.isInvalid() || dest.isInvalid()) {
			return false;
		}
		return fs.rename(this, dest);
	}

	public boolean setLastModified(long time) {
		if (time < 0)
			throw new IllegalArgumentException("Negative time");
		if (isInvalid()) {
			return false;
		}
		return fs.setLastModifiedTime(this, time);
	}

	public boolean setReadOnly() {
		if (isInvalid()) {
			return false;
		}
		return fs.setReadOnly(this);
	}

	public boolean setWritable(boolean writable, boolean ownerOnly) {
		if (isInvalid()) {
			return false;
		}
		return fs.setPermission(this, FileSystem.ACCESS_WRITE, writable, ownerOnly);
	}

	public boolean setWritable(boolean writable) {
		return setWritable(writable, true);
	}

	public boolean setReadable(boolean readable, boolean ownerOnly) {
		if (isInvalid()) {
			return false;
		}
		return fs.setPermission(this, FileSystem.ACCESS_READ, readable, ownerOnly);
	}

	public boolean setReadable(boolean readable) {
		return setReadable(readable, true);
	}

	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		if (isInvalid()) {
			return false;
		}
		return fs.setPermission(this, FileSystem.ACCESS_EXECUTE, executable, ownerOnly);
	}

	public boolean setExecutable(boolean executable) {
		return setExecutable(executable, true);
	}

	public boolean canExecute() {
		if (isInvalid()) {
			return false;
		}
		return fs.checkAccess(this, FileSystem.ACCESS_EXECUTE);
	}

	public static File[] listRoots() {
		return fs.listRoots();
	}

	public long getTotalSpace() {
		if (isInvalid()) {
			return 0L;
		}
		return fs.getSpace(this, FileSystem.SPACE_TOTAL);
	}

	public long getFreeSpace() {
		if (isInvalid()) {
			return 0L;
		}
		return fs.getSpace(this, FileSystem.SPACE_FREE);
	}

	public long getUsableSpace() {
		if (isInvalid()) {
			return 0L;
		}
		return fs.getSpace(this, FileSystem.SPACE_USABLE);
	}

	private static class TempDirectory {
		private TempDirectory() {
		}

		// temporary directory location
		private static final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

		static File location() {
			return tmpdir;
		}

		static File generateFile(String prefix, String suffix, File dir) throws IOException {
			long n = (long) Math.random() * Long.MAX_VALUE;
			if (n == Long.MIN_VALUE) {
				n = 0; // corner case
			} else {
				n = Math.abs(n);
			}

			// Use only the file name from the supplied prefix
			prefix = (new File(prefix)).getName();

			String name = prefix + Long.toString(n) + suffix;
			File f = new File(dir, name);
			if (!name.equals(f.getName()) || f.isInvalid()) {
				throw new IOException("Unable to create temporary file, " + f);
			}
			return f;
		}
	}

	public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
		if (prefix.length() < 3)
			throw new IllegalArgumentException("Prefix string too short");
		if (suffix == null)
			suffix = ".tmp";

		File tmpdir = (directory != null) ? directory : TempDirectory.location();
		File f;
		do {
			f = TempDirectory.generateFile(prefix, suffix, tmpdir);

		} while ((fs.getBooleanAttributes(f) & FileSystem.BA_EXISTS) != 0);

		if (!fs.createFileExclusively(f.getPath()))
			throw new IOException("Unable to create temporary file");

		return f;
	}

	public static File createTempFile(String prefix, String suffix) throws IOException {
		return createTempFile(prefix, suffix, null);
	}

	public int compareTo(File pathname) {
		return fs.compare(this, pathname);
	}

	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof File)) {
			return compareTo((File) obj) == 0;
		}
		return false;
	}

	public int hashCode() {
		return fs.hashCode(this);
	}

	public String toString() {
		return getPath();
	}

	private static final long serialVersionUID = 301077366599181567L;

}
