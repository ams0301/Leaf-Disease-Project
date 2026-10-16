let express=require('express')
let router=express.Router()
let constants=require('../constants')
let multer=require('multer')
let upload=multer({dest:'image/'})
const spawn=require('child_process').spawn

router.post('/',upload.single('image'),function(req,res,next){

    if(!req.file){
        res.status(400).send({error:'No image file was uploaded'})
        return
    }

    let path=req.file.path

    const pythonProcess=spawn('python',['scripts/image_preprocess.py',path,constants.idx1,constants.idx2,constants.idx3])

    let stdoutData=''

    let responded=false

    const respond=function(status,body){
        if(responded)
            return
        responded=true
        res.status(status).send(body)
    }

    pythonProcess.stdout.on('data',(data)=>{
        stdoutData+=data
    })

    pythonProcess.stderr.on('data',(data)=>{
        console.log(`stderr: ${data}`)
    })

    pythonProcess.on('error',(err)=>{
        console.log(`Failed to start python process : ${err.message}`)
        respond(500,{error:'Failed to run prediction process'})
    })

    pythonProcess.on('close',(code)=>{
        console.log(`Process quit with code ${code}`)
        try{
            let plant=JSON.parse(stdoutData)
            respond(200,plant)
        }
        catch(err){
            console.log(`Error while parsing prediction output : ${err.message}`)
            respond(500,{error:'Failed to get prediction from model'})
        }
    })

});

module.exports=router