package pt.up.edscrum.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    // Pasta onde as imagens serão guardadas (na raiz do projeto)
    private final String UPLOAD_DIR = "uploads/profile-images/";

    public FileStorageService() {
        try {
            // Cria a pasta se ela não existir
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar a pasta de uploads!");
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        // Gera um nome único para evitar ficheiros com o mesmo nome (ex: uuid_avatar.png)
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // Define o caminho completo onde guardar
        Path destinationPath = Paths.get(UPLOAD_DIR + fileName);

        // Copia o ficheiro para a pasta
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
}
