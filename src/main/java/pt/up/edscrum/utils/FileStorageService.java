package pt.up.edscrum.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Serviço utilitário responsável pelo armazenamento de ficheiros enviados
 * (por exemplo imagens de perfil) no sistema de ficheiros local.
 */
@Service
public class FileStorageService {

    /**
     * Pasta onde as imagens serão guardadas (relativa à raiz do projeto).
     */
    private final String UPLOAD_DIR = "uploads/profile-images/";

    /**
     * Inicializa o serviço de armazenamento de ficheiros garantindo que a
     * diretoria de uploads existe.
     */
    public FileStorageService() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar a pasta de uploads!");
        }
    }

    /**
     * Guarda o ficheiro enviado para a pasta de uploads com um nome único.
     *
     * @param file MultipartFile enviado pelo formulário
     * @return Nome do ficheiro guardado (ou null se o ficheiro estiver vazio)
     * @throws IOException Em caso de erro ao copiar o ficheiro
     */
    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        Path destinationPath = Paths.get(UPLOAD_DIR + fileName);

        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
}
