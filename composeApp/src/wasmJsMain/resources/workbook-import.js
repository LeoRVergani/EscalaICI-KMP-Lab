(function () {
  const sheetSeparator = "\u001D";
  const rowSeparator = "\u001E";
  const cellSeparator = "\u001F";

  function encodeCell(value) {
    return encodeURIComponent(String(value == null ? "" : value));
  }

  function workbookToPayload(workbook) {
    return workbook.SheetNames.map((sheetName) => {
      const sheet = workbook.Sheets[sheetName];
      const rows = XLSX.utils.sheet_to_json(sheet, {
        header: 1,
        defval: "",
        raw: false,
        blankrows: true
      });
      return encodeCell(sheetName) + cellSeparator + rows
        .map((row) => row.map(encodeCell).join(cellSeparator))
        .join(rowSeparator);
    }).join(sheetSeparator);
  }

  globalThis.escalaIciParseWorkbookBase64 = function (base64, fileName, callback) {
    if (!globalThis.XLSX) {
      callback("error", fileName, "Biblioteca XLSX indisponível. Verifique a conexão e recarregue a página.");
      return;
    }
    try {
      const binary = atob(base64);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
      }
      const workbook = XLSX.read(bytes, { type: "array", cellDates: false });
      callback("success", fileName, workbookToPayload(workbook));
    } catch (error) {
      callback("error", fileName, error && error.message ? error.message : "Falha ao processar a planilha.");
    }
  };

  globalThis.escalaIciOpenWorkbookPicker = function (callback) {
    if (!globalThis.XLSX) {
      callback("error", "", "Biblioteca XLSX indisponível. Verifique a conexão e recarregue a página.");
      return;
    }

    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    input.style.display = "none";
    document.body.appendChild(input);

    input.addEventListener("change", function () {
      const file = input.files && input.files[0];
      if (!file) {
        input.remove();
        return;
      }

      const reader = new FileReader();
      reader.onload = function () {
        try {
          const data = new Uint8Array(reader.result);
          const workbook = XLSX.read(data, { type: "array", cellDates: false });
          callback("success", file.name, workbookToPayload(workbook));
        } catch (error) {
          callback("error", file.name, error && error.message ? error.message : "Falha ao processar a planilha.");
        } finally {
          input.remove();
        }
      };
      reader.onerror = function () {
        callback("error", file.name, "Falha ao ler o arquivo selecionado.");
        input.remove();
      };
      reader.readAsArrayBuffer(file);
    });

    input.click();
  };
})();
