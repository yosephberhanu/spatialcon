package et.yoseph.spatialcon.conversion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import et.yoseph.spatialcon.common.StorageService;
import et.yoseph.spatialcon.exceptions.StorageException;
import et.yoseph.spatialcon.exceptions.StorageFileNotFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ConvertService implements StorageService {
    private final Path rootLocation;
    private final Path command;

    @Autowired
    public ConvertService(ConvertProperties properties) {

        if (properties.getLocation().trim().length() == 0) {
            throw new StorageException("File upload location can not be Empty.");
        }

        this.rootLocation = Paths.get(properties.getLocation());
        this.command = Paths.get(properties.getCommand());

    }

    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }
            Path destinationFile = this.rootLocation.resolve(
                    Paths.get(file.getOriginalFilename()))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    private Path convert(String inputFileName, String type) throws InterruptedException, IOException {
        String option = "-2shp";
        if (type.equalsIgnoreCase("shp")) {
            option = "-2shp";
        } else if (type.equalsIgnoreCase("psql")) {
            option = "-2psql";
        }
        Path inputFilePath = this.rootLocation.resolve(
                Paths.get(inputFileName))
                .normalize().toAbsolutePath();

        Path outputDirPath = this.rootLocation.resolve(
                Paths.get(UUID.randomUUID().toString()))
                .normalize().toAbsolutePath();
        Files.createDirectories(outputDirPath);

        // Process proc = Runtime.getRuntime()
        // .exec(new String[] { "mkdir", "-m", "777",
        // "/Users/yoseph/Work/Personal/storage/test" });

        ProcessBuilder pb = new ProcessBuilder(command.toString(),
                " -d " + outputDirPath.toString() + " " + option + " " + inputFilePath.toString());

        Process process = pb.start();
        System.out.println("Done .......");
        return outputDirPath;

    }

    private List<Path> populateFilesList(Path dir) throws IOException {
        List<Path> filesListInDir = new ArrayList<>();
        List<Path> files = Files.list(dir).toList();
        for (Path file : files) {
            filesListInDir.add(file.toAbsolutePath());
            // else
            // populateFilesList(file);
        }
        return filesListInDir;
    }

    private Path zip(Path outputDirPath) {
        Path output = rootLocation.resolve(UUID.randomUUID().toString() + ".zip");
        try {
            List<Path> filesListInDir = populateFilesList(outputDirPath);
            // now zip files one by one
            // create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(output.toFile());
            ZipOutputStream zos = new ZipOutputStream(fos);
            for (Path filePath : filesListInDir) {
                // for ZipEntry we need to keep only relative file path, so we used substring on
                // absolute path
                ZipEntry ze = new ZipEntry(filePath.getFileName().toString());
                zos.putNextEntry(ze);
                // read the file and write to ZipOutputStream
                FileInputStream fis = new FileInputStream(filePath.toFile());
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public Resource process(String inputFileName, String type) {
        try {

            System.out.println("Started converting");
            Path outputDirPath = convert(inputFileName, type);
            System.out.println("Done converting");
            Path outputFilePath = zip(outputDirPath);
            System.out.println("Done zipping");
            Resource resource = new UrlResource(outputFilePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + outputFilePath);

            }
        } catch (InterruptedException | IOException e) {
            System.out.println("$#$###$$$#$$#$$$$$#$$");
            System.out.println(e);
            System.out.println("$#$###$$$#$$#$$$$$#$$");
            throw new StorageFileNotFoundException("Could not read file: " + inputFileName, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
