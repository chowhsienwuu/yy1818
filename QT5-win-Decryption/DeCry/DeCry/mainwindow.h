#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>

namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = 0);
    ~MainWindow();

private:
    Ui::MainWindow *ui;

    QByteArray passwd2sha512(const char * str);
    void decryptionFile(const char *src, const char *dest, const char * key, int keylen);
    char buffer_src_file[256];
    char buffer_dest_file[256];
private slots:
    void openFile();
    void chooseDestFile();
    void begin();
};

#endif // MAINWINDOW_H
