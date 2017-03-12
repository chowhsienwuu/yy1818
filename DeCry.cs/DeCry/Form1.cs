using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Security.Cryptography;
using System.IO;

namespace DeCry
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();

        }

        private string srcFilePath = null;
        private string destFilePath = null;
        private string passwd = "";
        private SHA512 sha512 = new SHA512CryptoServiceProvider();
        private void openFileButton_Click(object sender, EventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.ShowDialog();
            debug(ofd.FileName);

            srcFilePath = ofd.FileName;
            srcFilePathText.Text = srcFilePath;
        }

        private void saveAsButton_Click(object sender, EventArgs e)
        {
            SaveFileDialog sfd = new SaveFileDialog();
            sfd.ShowDialog();

            destFilePath = sfd.FileName;
            saveAsFilePathText.Text = destFilePath;
        }

        private void begainButton_Click(object sender, EventArgs e)
        {
            passwd = passwdText.Text;
            sbyte[] block =  passwd2sha512(passwd);
            /*文件路径可能被手动改变*/
            srcFilePath = srcFilePathText.Text;
            destFilePath =saveAsFilePathText.Text;
            decryptionFile(srcFilePath, destFilePath, block);
        }

        static void debug(string arg)
        {
            Console.WriteLine("" + arg);
        }

        private sbyte[] passwd2sha512(string args)
        {
            if (args.Length < 1)
            {
                args = "";
            }
            byte[] byteVale = System.Text.Encoding.UTF8.GetBytes(args);
            byte[] retVal = sha512.ComputeHash(byteVale);

            sbyte[] retsbyte = new sbyte[retVal.Length];
            for (int i = 0; i < retVal.Length; i++)
            {
                retsbyte[i] = (sbyte)retVal[i];
            }

            return retsbyte;
        }

        private void decryptionFile(string src, string dest, sbyte[] block)
        {
            FileInfo srcFileinfo = new FileInfo(src);
            FileInfo destFileinfo = null;
            if (!srcFileinfo.Exists)
            {
                debug("src file not exits");
                return;
            }
            statusProgress.Maximum = (int)srcFileinfo.Length;
            statusLable.Text = "0%";
            BinaryReader br = new BinaryReader(srcFileinfo.OpenRead());
            // del 
            if (File.Exists(dest))
            {
                File.Delete(dest);

            }
            destFileinfo = new FileInfo(dest);
           // destFileinfo.Create();
            FileStream destFs = destFileinfo.OpenWrite();
            BinaryWriter bw = new BinaryWriter(destFs);

            sbyte[] buffer = new sbyte[1024];
            byte[] swap = null;
            sbyte[] buffer_decode = new sbyte[1024];

            for (int i = 0; i < buffer_decode.Length; i++)
            {
                buffer_decode[i] = block[i % block.Length];
            }
            //just copy the head '44byte

            swap = br.ReadBytes(44);
            bw.Write(swap);

            long cap = srcFileinfo.Length / 1024 / 100; //100 times to update progressbar. otherwise big
            cap = (cap == 0 ? 2 : cap);
            //files make this app scrash.
            int k = 0;
            debug("the cap is " + cap);
            
            while (swap.Length > 0)
            {
                swap = br.ReadBytes(1024);
                //debug("the swap is : " + swap.Length);
                for (int i = 0; i < swap.Length; i++)
                {
                    if (i % 2 == 0)
                    {
                        buffer[i] = (sbyte)((sbyte)swap[i] - buffer_decode[i]);
                    }
                    else
                    {
                        buffer[i] = (sbyte)((sbyte)swap[i] + buffer_decode[i]);
                    }
                }
                for (int i = 0;  i < swap.Length; i++)
                {
                    swap[i] = (byte)buffer[i];
                }

                bw.Write(swap);
                bw.Flush();
              
                if (k++ % cap == 1)
                {
                    statusProgress.Value = (int)bw.BaseStream.Position;
                    debug(" " + cap + " " + k + " " + k * cap * 1.0 / statusProgress.Maximum);
                  //  statusLable.Text = "" + (int)(statusProgress.Value * 100.0 / statusProgress.Maximum) + "%";
                }
            }
            br.Dispose();
            bw.Dispose();
            br.Close();
            bw.Close();

        }

    }
}
