using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

internal static class RitmoLauncher
{
    [STAThread]
    private static int Main(string[] args)
    {
        try
        {
            var directory = AppDomain.CurrentDomain.BaseDirectory;
            var java = Path.Combine(directory, @"runtime\bin\javaw.exe");
            var app = Path.Combine(directory, @"app\ritmo.jar");
            var modules = Path.Combine(directory, @"app\javafx");

            if (!File.Exists(java) || !File.Exists(app) || !Directory.Exists(modules))
                throw new InvalidOperationException("Extrae todo el archivo ZIP y conserva las carpetas app y runtime junto a Ritmo.exe.");

            var verify = args.Length == 1 && args[0] == "--test-views";
            var cache = Path.Combine(Path.GetTempPath(), "Ritmo-JavaFX");
            var command = Quote("-Djavafx.cachedir=" + cache)
                + " --module-path " + Quote(modules)
                + " --add-modules javafx.controls -cp " + Quote(app)
                + " academia.Main" + (verify ? " --test-views" : "");
            var start = new ProcessStartInfo(java, command);
            start.UseShellExecute = false;
            start.WorkingDirectory = directory;
            using (var process = Process.Start(start))
            {
                process.WaitForExit();
                if (process.ExitCode != 0 && !verify)
                    throw new InvalidOperationException("La aplicación terminó con el código " + process.ExitCode + ".");
                return process.ExitCode;
            }
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "Ritmo", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    private static string Quote(string value)
    {
        return "\"" + value + "\"";
    }
}
